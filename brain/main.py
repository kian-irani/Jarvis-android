from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
import asyncio, json, uuid, time, psutil, aiohttp
from typing import Dict, List

app = FastAPI(title="JARVIS Brain", version="2.0.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

nodes: Dict[str, dict] = {}
node_sockets: Dict[str, WebSocket] = {}

# Groq Key Rotation
GROQ_KEYS: List[str] = [
    "GROQ_KEY_1",
    "GROQ_KEY_2",
    "GROQ_KEY_3",
]
groq_index = 0

async def ask_groq(message: str, system: str = "You are JARVIS, a helpful AI assistant.") -> str:
    global groq_index
    for _ in range(len(GROQ_KEYS)):
        key = GROQ_KEYS[groq_index % len(GROQ_KEYS)]
        try:
            async with aiohttp.ClientSession() as s:
                r = await s.post(
                    "https://api.groq.com/openai/v1/chat/completions",
                    headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
                    json={"model": "llama-3.3-70b-versatile", "messages": [
                        {"role": "system", "content": system},
                        {"role": "user", "content": message}
                    ], "max_tokens": 1024},
                    timeout=aiohttp.ClientTimeout(total=30)
                )
                data = await r.json()
                if "choices" in data:
                    return data["choices"][0]["message"]["content"]
                elif data.get("error", {}).get("code") == "rate_limit_exceeded":
                    print(f"⚠️ Key {groq_index % len(GROQ_KEYS) + 1} محدود شد — کلید بعدی")
                    groq_index += 1
                else:
                    return f"خطا: {data.get('error', {}).get('message', 'نامشخص')}"
        except Exception as e:
            print(f"⚠️ خطا: {e}")
            groq_index += 1
    return "همه کلیدها محدود شدن — بعداً تلاش کن"

@app.websocket("/node/connect")
async def node_connect(ws: WebSocket):
    await ws.accept()
    node_id = None
    try:
        data = await ws.receive_text()
        info = json.loads(data)
        node_id = info.get("node_id", str(uuid.uuid4()))
        info["connected_at"] = time.time()
        info["status"] = "online"
        nodes[node_id] = info
        node_sockets[node_id] = ws
        print(f"✅ Node: {info.get('name')} [{info.get('device_type')}]")
        await ws.send_text(json.dumps({"status": "registered", "node_id": node_id}))
        while True:
            try:
                msg = await asyncio.wait_for(ws.receive_text(), timeout=15)
                data = json.loads(msg)
                if data.get("type") == "heartbeat":
                    nodes[node_id]["last_seen"] = time.time()
                    nodes[node_id]["metrics"] = data.get("metrics", {})
                    nodes[node_id]["status"] = "online"
                    await ws.send_text(json.dumps({"type": "pong"}))
                elif data.get("type") == "task_result":
                    print(f"📦 {nodes[node_id]['name']}: {data.get('result','')[:100]}")
            except asyncio.TimeoutError:
                if node_id in nodes:
                    nodes[node_id]["status"] = "idle"
    except WebSocketDisconnect:
        pass
    finally:
        if node_id and node_id in nodes:
            nodes[node_id]["status"] = "offline"
            node_sockets.pop(node_id, None)

@app.get("/")
async def root():
    online = len([n for n in nodes.values() if n.get("status") == "online"])
    return {"message": "🤖 JARVIS Brain v2.0 Online", "nodes_online": online, "nodes_total": len(nodes), "ai": "Groq LLaMA 3.3 70B"}

@app.get("/nodes")
async def get_nodes():
    return {"nodes": list(nodes.values()), "count": len(nodes)}

@app.post("/nodes/{node_id}/exec")
async def exec_command(node_id: str, body: dict):
    if node_id not in node_sockets:
        return {"error": "Node آفلاین است"}
    await node_sockets[node_id].send_text(json.dumps({
        "type": "exec", "command": body.get("command"), "task_id": str(uuid.uuid4())
    }))
    return {"status": "sent"}

@app.post("/chat")
async def chat(body: dict):
    message = body.get("message", "")
    if not message:
        return {"error": "پیام خالی"}
    print(f"💬 chat: {message}")
    response = await ask_groq(message)
    return {"response": response, "model": "llama-3.3-70b", "key_index": groq_index % len(GROQ_KEYS) + 1}

@app.get("/health")
async def health():
    m = psutil.virtual_memory()
    return {
        "status": "online",
        "cpu": psutil.cpu_percent(),
        "ram_free_gb": round(m.available/1e9, 2),
        "nodes_online": len([n for n in nodes.values() if n.get("status") == "online"]),
        "nodes_total": len(nodes),
        "groq_keys": len(GROQ_KEYS),
        "active_key": groq_index % len(GROQ_KEYS) + 1
    }
