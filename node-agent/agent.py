#!/usr/bin/env python3
import asyncio, json, uuid, time, platform, os, subprocess, psutil, websockets

BRAIN_WS  = os.environ.get("JARVIS_BRAIN", "ws://212.87.199.62:8000/node/connect")
NODE_NAME = os.environ.get("JARVIS_NAME", platform.node())
NODE_ID   = os.environ.get("JARVIS_ID", str(uuid.uuid4()))

def detect_type():
    if os.path.exists("/system/build.prop"): return "android"
    if platform.system() == "Windows": return "windows_pc"
    if platform.system() == "Darwin": return "macos"
    return "linux_vps"

def get_caps():
    caps = []
    try:
        subprocess.run(["docker","ps"], capture_output=True, check=True)
        caps.append("docker")
    except: pass
    try:
        import urllib.request
        urllib.request.urlopen("http://localhost:11434", timeout=1)
        caps.append("ollama")
    except: pass
    if psutil.cpu_count() >= 4: caps.append("compute")
    if psutil.disk_usage("/").free > 10*1e9: caps.append("storage")
    return caps

def get_metrics():
    m = psutil.virtual_memory()
    return {
        "cpu": psutil.cpu_percent(interval=0.1),
        "ram_free_gb": round(m.available/1e9, 2),
        "ram_percent": m.percent,
        "disk_free_gb": round(psutil.disk_usage("/").free/1e9, 2)
    }

def run_cmd(cmd):
    try:
        r = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=30)
        return r.stdout + r.stderr
    except Exception as e:
        return str(e)

async def connect():
    info = {
        "node_id": NODE_ID,
        "name": NODE_NAME,
        "device_type": detect_type(),
        "os": platform.system(),
        "capabilities": get_caps(),
        "hardware": {
            "cpu_cores": psutil.cpu_count(logical=False),
            "ram_gb": round(psutil.virtual_memory().total/1e9, 1),
            "disk_gb": round(psutil.disk_usage("/").total/1e9, 1)
        }
    }
    print(f"🤖 JARVIS Node: {NODE_NAME} [{info['device_type']}]")
    print(f"🔗 Brain: {BRAIN_WS}")
    while True:
        try:
            async with websockets.connect(BRAIN_WS) as ws:
                await ws.send(json.dumps(info))
                resp = json.loads(await ws.recv())
                print(f"✅ ثبت شد | ID: {resp.get('node_id')}")
                while True:
                    try:
                        msg = await asyncio.wait_for(ws.recv(), timeout=10)
                        d = json.loads(msg)
                        if d.get("type") == "exec":
                            cmd = d.get("command", "")
                            print(f"⚡ اجرا: {cmd}")
                            result = run_cmd(cmd)
                            await ws.send(json.dumps({
                                "type": "task_result",
                                "task_id": d.get("task_id"),
                                "result": result
                            }))
                    except asyncio.TimeoutError:
                        await ws.send(json.dumps({
                            "type": "heartbeat",
                            "metrics": get_metrics()
                        }))
        except Exception as e:
            print(f"⚠️ {e} — retry 5s")
            await asyncio.sleep(5)

if __name__ == "__main__":
    asyncio.run(connect())
