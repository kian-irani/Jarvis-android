#!/bin/bash
BRAIN_IP="212.87.199.62"
echo "🤖 JARVIS Node Installer"
pip3 install websockets psutil --break-system-packages 2>/dev/null || /opt/jarvis/venv/bin/pip install websockets psutil
mkdir -p /opt/jarvis-node
cp /opt/jarvis/node-agent/agent.py /opt/jarvis-node/agent.py
cat > /etc/systemd/system/jarvis-node.service << SERVICE
[Unit]
Description=JARVIS Node Agent
After=network.target
[Service]
ExecStart=/opt/jarvis/venv/bin/python /opt/jarvis-node/agent.py
Restart=always
RestartSec=5
Environment=JARVIS_BRAIN=ws://$BRAIN_IP:8000/node/connect
Environment=JARVIS_NAME=$(hostname)
[Install]
WantedBy=multi-user.target
SERVICE
systemctl daemon-reload
systemctl enable jarvis-node
systemctl start jarvis-node
echo "✅ Node نصب شد"
