#!/usr/bin/env bash
# W2 — Vision mesh node installer. One-liner for a bare Linux server (no Vision/Android):
#   curl -fsSL https://raw.githubusercontent.com/kian-irani/Jarvis-android/main/node-agent/install.sh \
#        | bash -s -- --host <brain-addr> --token <pairing-token> [--port 7799]
#
# Fetches agent.py (stdlib only — no pip), installs a systemd unit, and the agent registers the
# server's CPU/RAM/disk with the Brain over plain HTTP, re-posting a heartbeat every 30s.
set -euo pipefail

HOST=""
PORT="7799"
TOKEN=""
REPO_RAW="https://raw.githubusercontent.com/kian-irani/Jarvis-android/main/node-agent"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --host)  HOST="$2"; shift 2 ;;
    --port)  PORT="$2"; shift 2 ;;
    --token) TOKEN="$2"; shift 2 ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

if [ -z "$HOST" ] || [ -z "$TOKEN" ]; then
  echo "usage: install.sh --host <brain-addr> --token <token> [--port 7799]" >&2
  exit 2
fi

echo "🛰️  Vision node installer — brain ${HOST}:${PORT}"

command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }

INSTALL_DIR="/opt/vision-node"
mkdir -p "$INSTALL_DIR"

# Fetch the stdlib-only agent (prefer a local copy when run from a checkout).
if [ -f "$(dirname "$0")/agent.py" ]; then
  cp "$(dirname "$0")/agent.py" "$INSTALL_DIR/agent.py"
else
  curl -fsSL "$REPO_RAW/agent.py" -o "$INSTALL_DIR/agent.py"
fi

cat > /etc/systemd/system/vision-node.service <<SERVICE
[Unit]
Description=Vision Mesh Node Agent
After=network-online.target
Wants=network-online.target

[Service]
ExecStart=$(command -v python3) $INSTALL_DIR/agent.py --host ${HOST} --port ${PORT} --token ${TOKEN}
Restart=always
RestartSec=5
User=root

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable vision-node
systemctl restart vision-node

echo "✅ Vision node installed — systemctl status vision-node"
