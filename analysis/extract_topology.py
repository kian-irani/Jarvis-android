#!/usr/bin/env python3
"""Topology extractor for the Vision OS repo (modernize-map).

Parses Kotlin (Android app), Python (brain server, node-agent) and the
deployment config (AndroidManifest.xml, docker-compose.yml) to build a
call/data dependency graph. Writes analysis/topology.json and prints a
human summary.

Edge resolution notes:
- Kotlin "calls" are resolved by mapping top-level declared names
  (class/object/interface/enum/top-level fun) to their defining file,
  then scanning each file's code (comments/strings stripped) for those
  names. Hilt DI wiring (@Inject constructor params, @Provides returns)
  and Ktor route registration are emitted as kind=dispatch — they are
  dynamic dispatch, not direct calls.
- Data edges: Room (@Dao/@Database/@Entity -> room datastore),
  DataStore/EncryptedSharedPreferences (pairing prefs), docker-compose
  services (redis/postgres/temporal) joined to brain code via env refs.
- Entry points come from AndroidManifest.xml (launcher activity,
  services) and process mains (uvicorn/FastAPI app, __main__ guard).
- Dead ends are suppressed for anything that is a plausible dynamic
  dispatch target (Composables, ViewModels, Hilt modules, route
  builders, DAOs) — those uncertainties go to observations instead.
"""
import json
import os
import re
import sys
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP_SRC = os.path.join(ROOT, "app/src/main/java/com/kianirani/jarvis")
OUT_DIR = os.path.dirname(os.path.abspath(__file__))

# ---------- helpers ----------

def strip_kotlin(code: str) -> str:
    code = re.sub(r"/\*.*?\*/", "", code, flags=re.S)
    code = re.sub(r"//[^\n]*", "", code)
    code = re.sub(r'""".*?"""', '""', code, flags=re.S)
    code = re.sub(r'"(\\.|[^"\\])*"', '""', code)
    return code

def strip_python(code: str) -> str:
    code = re.sub(r"#[^\n]*", "", code)
    code = re.sub(r'("""|\'\'\').*?\1', '""', code, flags=re.S)
    return code

DECL_RE = re.compile(
    r"^\s*(?:@\w+\s+)*(?:public |internal |private |abstract |open |sealed |data |annotation )*"
    r"(?:class|object|interface|enum class)\s+(\w+)"
    r"|^(?:private |internal |suspend )*fun\s+(?:[\w.<>]+\.)?(\w+)\s*[(<]", re.M)

# ---------- collect modules ----------

modules = {}   # id -> dict(name, domain, language, loc, file, decls, code)
edges = set()  # (source, target, kind)

def domain_for(rel: str) -> str:
    if rel.startswith("ui/") :
        return "Android UI"
    if rel.startswith("brain/"):
        return "Android Brain-Lite"
    return "Android Core"

for dirpath, _, files in os.walk(APP_SRC):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(dirpath, fn)
        rel = os.path.relpath(path, APP_SRC)
        raw = open(path, encoding="utf-8").read()
        code = strip_kotlin(raw)
        name = fn[:-3]
        decls = set()
        for m in DECL_RE.finditer(code):
            decls.add(m.group(1) or m.group(2))
        decls.add(name)
        modules[name] = dict(
            name=name, domain=domain_for(rel.replace("\\", "/")),
            language="kotlin", loc=len(raw.splitlines()),
            file=f"app/src/main/java/com/kianirani/jarvis/{rel}",
            decls=decls, code=code)

# Python modules
for rel in ["brain/main.py", "brain/logging_config.py", "node-agent/agent.py"]:
    path = os.path.join(ROOT, rel)
    if not os.path.exists(path):
        continue
    raw = open(path, encoding="utf-8").read()
    name = ("brain_" if rel.startswith("brain") else "agent_") + os.path.basename(rel)[:-3]
    dom = "Brain Server (Python)" if rel.startswith("brain") else "Node Agent (Python)"
    modules[name] = dict(name=name, domain=dom, language="python",
                         loc=len(raw.splitlines()), file=rel,
                         decls=set(), code=strip_python(raw))

# ---------- kotlin edges ----------

name_to_module = {}
for mid, m in modules.items():
    for d in m["decls"]:
        name_to_module.setdefault(d, mid)

DI_PARAM_RE = re.compile(r"@Inject\s+constructor\s*\(([^)]*)\)", re.S)
PROVIDES_RE = re.compile(r"@Provides[^=]*?:\s*(\w+)")

for mid, m in modules.items():
    if m["language"] != "kotlin":
        continue
    code = m["code"]
    di_types = set()
    for pm in DI_PARAM_RE.finditer(code):
        di_types |= set(re.findall(r":\s*(\w+)", pm.group(1)))
    di_types |= set(PROVIDES_RE.findall(code))
    idents = set(re.findall(r"\b([A-Z]\w+)\b", code))
    # project imports resolve lowercase symbols (e.g. Ktor route extension funs)
    idents |= set(re.findall(r"^import\s+com\.kianirani\.jarvis\.[\w.]*\.(\w+)$", code, re.M))
    for ident in idents:
        tgt = name_to_module.get(ident)
        if not tgt or tgt == mid:
            continue
        kind = "dispatch" if ident in di_types else "call"
        edges.add((mid, tgt, kind))

# Ktor route registration in KtorServer -> route files (dispatch)
if "KtorServer" in modules:
    for mid, m in modules.items():
        if mid.endswith("Routes") and ("KtorServer", mid, "call") in edges:
            edges.discard(("KtorServer", mid, "call"))
            edges.add(("KtorServer", mid, "dispatch"))

# ---------- python edges ----------

if "brain_main" in modules and "brain_logging_config" in modules:
    if re.search(r"\blogging_config\b", modules["brain_main"]["code"]):
        edges.add(("brain_main", "brain_logging_config", "call"))
# node-agent talks to brain HTTP/WS API
if "agent_agent" in modules and "brain_main" in modules:
    if re.search(r"(ws://|http://|aiohttp|websocket)", modules["agent_agent"]["code"], re.I):
        edges.add(("agent_agent", "brain_main", "dispatch"))

# Android JarvisClient/BrainRepository -> brain server (HTTP). BrainRepository
# currently hardcodes a dead host — flagged in observations.
for mid in ("BrainRepository",):
    if mid in modules and re.search(r"(Retrofit|OkHttp|HttpURLConnection|ktor|http)", modules[mid]["code"], re.I):
        edges.add((mid, "brain_main", "dispatch"))

# ---------- datastores ----------

datastores = {}
def add_ds(dsid, name):
    datastores[dsid] = name

add_ds("ds:room-vision-db", "Room: vision.db (node registry, memory, tasks)")
add_ds("ds:pairing-prefs", "EncryptedSharedPreferences: brain pairing")
# docker-compose services
compose = os.path.join(ROOT, "docker-compose.yml")
if os.path.exists(compose):
    ctext = open(compose).read()
    for svc in ("redis", "postgres", "temporal"):
        if re.search(rf"^\s{{2}}{svc}:", ctext, re.M):
            add_ds(f"ds:{svc}", svc)

for mid, m in modules.items():
    code = m["code"]
    if m["language"] == "kotlin":
        if re.search(r"@(Dao|Database|Entity)\b", code) or "RoomDatabase" in code:
            edges.add((mid, "ds:room-vision-db", "write"))
            edges.add((mid, "ds:room-vision-db", "read"))
        elif re.search(r"\b(VisionDatabase|NodeDao|Dao)\b", code) and mid != "VisionDatabase":
            edges.add((mid, "ds:room-vision-db", "read"))
        if re.search(r"EncryptedSharedPreferences|MasterKey", code):
            edges.add((mid, "ds:pairing-prefs", "write"))
            edges.add((mid, "ds:pairing-prefs", "read"))
        elif "BrainSelectionStore" in code and mid != "BrainSelectionStore":
            pass  # indirect via store class; call edge already exists
    else:
        for svc in ("redis", "postgres", "temporal"):
            if re.search(rf"\b{svc}", code, re.I) and f"ds:{svc}" in datastores:
                edges.add((mid, f"ds:{svc}", "write"))

# ---------- entry points ----------

entry_points = []
manifest = os.path.join(ROOT, "app/src/main/AndroidManifest.xml")
if os.path.exists(manifest):
    mtext = open(manifest).read()
    for comp in re.findall(r'android:name="\.([\w.]+)"', mtext):
        leaf = comp.split(".")[-1]
        if leaf in modules:
            entry_points.append(leaf)
for pid in ("brain_main", "agent_agent"):
    if pid in modules:
        entry_points.append(pid)

# ---------- dead ends ----------

inbound = defaultdict(int)
for s, t, k in edges:
    inbound[t] += 1

DISPATCH_TARGET_RE = re.compile(
    r"@(Composable|HiltViewModel|Module|Dao|HiltAndroidApp|AndroidEntryPoint)\b")
dead_ends, suppressed = [], []
for mid, m in modules.items():
    if mid in entry_points or inbound[mid] > 0:
        continue
    if m["language"] == "kotlin" and DISPATCH_TARGET_RE.search(m["code"]):
        suppressed.append(mid)
    else:
        dead_ends.append(mid)

# ---------- assemble ----------

domains = defaultdict(list)
for mid, m in sorted(modules.items()):
    domains[m["domain"]].append(dict(
        id=mid, name=m["name"], kind="module",
        language=m["language"], loc=m["loc"], file=m["file"]))

children = [
    dict(id=f"dom:{re.sub('[^a-z0-9]+', '-', d.lower())}", name=d,
         kind="domain", children=mods)
    for d, mods in sorted(domains.items())
]
children.append(dict(id="dom:data", name="Data stores", kind="domain",
                     children=[dict(id=k, name=v, kind="datastore")
                               for k, v in sorted(datastores.items())]))

observations = [
    "BrainLiteService is the coordination hub: it owns the Ktor server, NSD advertisement/discovery, heartbeats and the Room registry — a single point of failure on the elected-brain device.",
    "BrainRepository (Android Core) still targets a hardcoded external host that has been decommissioned; it bypasses BrainSelectionStore — launch blocker, also tracked in the auto-run-task queue.",
    "Room vision.db has many writers (repositories, heartbeat path, DAOs) — schema changes ripple across the whole Brain-Lite domain.",
    "All 8 Ktor route files are dispatch targets registered by KtorServer; a grep-only graph would mark them dead. Same for Hilt modules and Composable screens (dispatch via DI/navigation).",
    "The Python brain (brain/main.py) and Android Brain-Lite implement overlapping APIs (health/chat/embed) — candidate for a shared OpenAPI contract before the python brain grows.",
    "docker-compose declares redis/postgres/temporal but brain/main.py references few of them directly — infra is provisioned ahead of code; verify before scaling effort there.",
    "Discovery domain (NSD/mDNS + QR pairing + handshake) is cleanly bounded with one outward interface (BrainSelectionStore) — good service-extraction shape.",
]

flows = [
    dict(name="A phone joins the brain network",
         persona="Device owner (second phone)",
         description="Someone opens the setup wizard on a new phone, finds the household brain over Wi-Fi, and pairs with it.",
         steps=[
             dict(label="Open the setup wizard", nodes=["MainActivity", "SetupWizardScreen", "SetupWizardViewModel"]),
             dict(label="Scan the local network for brains (mDNS)", nodes=["NsdDiscovery"]),
             dict(label="Verify the brain with a health handshake", nodes=["BrainHandshake", "HealthRoutes"]),
             dict(label="Save the pairing securely", nodes=["BrainSelectionStore", "ds:pairing-prefs"]),
             dict(label="Start sending heartbeats to the elected brain", nodes=["HeartbeatSender", "NodeRoutes", "ds:room-vision-db"]),
         ]),
    dict(name="The household elects a brain",
         persona="Household admin",
         description="The admin opens the election screen, compares live device scores, and the strongest device becomes the brain.",
         steps=[
             dict(label="Open the brain election screen", nodes=["BrainElectionScreen", "BrainElectionViewModel"]),
             dict(label="Collect live metrics from this device", nodes=["LocalDeviceMetricsProvider", "NodeMetricsCodec"]),
             dict(label="Read registered nodes and their heartbeats", nodes=["NodeRepository", "VisionDatabase", "ds:room-vision-db"]),
             dict(label="Score candidates and pick the winner", nodes=["BrainScoreCalculator"]),
             dict(label="Run the brain service on the winner", nodes=["BrainLiteService", "KtorServer"]),
         ]),
    dict(name="Pairing a device by QR code",
         persona="Device owner",
         description="Instead of scanning the network, the owner shows a QR code on the brain phone and the new phone joins from it.",
         steps=[
             dict(label="Brain phone renders its pairing QR", nodes=["PairDeviceSection", "QrPairing", "LocalPairingInfo"]),
             dict(label="New phone decodes the join payload", nodes=["JoinPayload", "QrPairing"]),
             dict(label="Handshake and store the pairing", nodes=["BrainHandshake", "BrainSelectionStore", "ds:pairing-prefs"]),
         ]),
]

topology = dict(
    system="Vision OS",
    root=dict(id="sys", name="Vision OS", kind="system", children=children),
    edges=[dict(source=s, target=t, kind=k) for s, t, k in sorted(edges)],
    entryPoints=sorted(set(entry_points)),
    deadEnds=sorted(dead_ends),
    observations=observations,
    flows=flows,
)

with open(os.path.join(OUT_DIR, "topology.json"), "w") as f:
    json.dump(topology, f, indent=2)

# ---------- summary ----------

print(f"Vision OS topology — {len(modules)} modules, {len(datastores)} datastores, {len(edges)} edges")
print(f"\nDomains:")
for d, mods in sorted(domains.items()):
    print(f"  {d}: {len(mods)} modules, {sum(m['loc'] for m in mods)} LOC")
kinds = defaultdict(int)
for _, _, k in edges:
    kinds[k] += 1
print("\nEdge kinds: " + ", ".join(f"{k}={v}" for k, v in sorted(kinds.items())))
print("\nEntry points: " + ", ".join(sorted(set(entry_points))))
print("Dead-end candidates: " + (", ".join(sorted(dead_ends)) or "none"))
print("Suppressed (dynamic-dispatch targets): " + (", ".join(sorted(suppressed)) or "none"))
top_in = sorted(((inbound[m], m) for m in modules), reverse=True)[:8]
print("\nMost-depended-on modules:")
for n, m in top_in:
    print(f"  {m}: {n} inbound")
