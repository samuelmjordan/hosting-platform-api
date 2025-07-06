#!/bin/bash
set -e

# minimal mc-router setup
MCROUTER_VERSION="1.20.0"

echo "setting up mc-router..."

# create user
useradd -r -s /bin/false mcrouter 2>/dev/null || true

# create directories
mkdir -p /opt/mcrouter
chown mcrouter:mcrouter /opt/mcrouter

# download binary
ARCH=$(uname -m | sed 's/x86_64/amd64/; s/aarch64/arm64/')
wget -qO- "https://github.com/itzg/mc-router/releases/download/$MCROUTER_VERSION/mc-router_${MCROUTER_VERSION}_linux_${ARCH}.tar.gz" | tar -xzC /tmp
mv /tmp/mc-router /opt/mcrouter/
chmod +x /opt/mcrouter/mc-router

# systemd service
cat > /etc/systemd/system/mcrouter.service << 'EOF'
[Unit]
Description=Minecraft Router
After=network.target

[Service]
Type=simple
User=mcrouter
ExecStart=/opt/mcrouter/mc-router -port 25565 -api-binding 127.0.0.1:29999
Restart=always

[Install]
WantedBy=multi-user.target
EOF

# start it
systemctl daemon-reload
systemctl enable --now mcrouter

echo "done. api available at http://127.0.0.1:29999/routes"
echo "add routes with: curl -X POST http://127.0.0.1:29999/routes -H 'Content-Type: application/json' -d '{\"serverAddress\": \"test.com\", \"backend\": \"localhost:25565\"}'"