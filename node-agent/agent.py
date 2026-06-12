#!/usr/bin/env python3
"""Vision mesh node agent — stdlib only, no pip installs.

Joins a Vision Brain-Lite over plain HTTP (the brain has no WebSocket):
    python3 agent.py --host 192.168.1.20 --port 7799 --token <pairing-token>

Registers via POST /nodes with a stable id and re-posts every 30s as a
heartbeat so the brain's last_seen stays fresh.
"""
import argparse
import json
import os
import platform
import socket
import sys
import time
import urllib.error
import urllib.request
import uuid

p = argparse.ArgumentParser(description="Vision mesh node agent (HTTP)")
p.add_argument("--host", default=os.environ.get("VISION_HOST"), help="brain host/ip")
p.add_argument("--port", default=os.environ.get("VISION_PORT", "7799"), help="brain port")
p.add_argument("--token", default=os.environ.get("VISION_TOKEN", ""), help="pairing token")
p.add_argument("--interval", type=int, default=30, help="heartbeat seconds")
args = p.parse_args()

if not args.host:
    sys.exit("error: --host is required (or set VISION_HOST)")

BASE = "http://%s:%s" % (args.host, args.port)
ID_FILE = os.path.expanduser("~/.vision-node-id")


def node_id():
    if os.path.exists(ID_FILE):
        return open(ID_FILE).read().strip()
    nid = str(uuid.uuid4())
    with open(ID_FILE, "w") as f:
        f.write(nid)
    return nid


def http_json(method, path, body=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if args.token:
        req.add_header("X-Vision-Token", args.token)
    data = json.dumps(body).encode() if body is not None else None
    with urllib.request.urlopen(req, data=data, timeout=10) as resp:
        return json.loads(resp.read().decode())


def main():
    nid = node_id()
    name = platform.node() or "node"
    try:
        address = socket.gethostbyname(socket.gethostname())
    except socket.gaierror:
        address = "0.0.0.0"
    print("[vision-node] brain=%s id=%s name=%s" % (BASE, nid[:8], name))

    health = http_json("GET", "/health")
    print("[vision-node] brain health: %s" % health)

    caps = json.dumps({"os": platform.system().lower(), "cpus": os.cpu_count() or 1,
                       "python": platform.python_version()})
    while True:
        try:
            r = http_json("POST", "/nodes", {"id": nid, "name": name, "address": address,
                                             "capabilities": caps, "brain_score": 10})
            print("[vision-node] heartbeat ok: %s" % r)
        except Exception as e:  # noqa: BLE001 — keep the node alive on any network hiccup
            print("[vision-node] heartbeat failed: %s — retrying in %ss" % (e, args.interval))
        time.sleep(args.interval)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n[vision-node] stopped")
