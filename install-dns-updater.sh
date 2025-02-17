#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if script is run as root
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Please run as root (use sudo)${NC}"
    exit 1
fi

# Installation directory
INSTALL_DIR="/opt/dns-updater"
SERVICE_NAME="dns-updater"
JAR_URL="https://github.com/yourusername/dns-updater/releases/latest/download/dns-updater.jar"

echo -e "${GREEN}Dynamic DNS Updater Installation Script${NC}"
echo "----------------------------------------"

# Function to prompt for configuration values
configure_dns_updater() {
    echo -e "${YELLOW}Please enter your Cloudflare configuration:${NC}"
    read -p "Cloudflare API Token: " CF_TOKEN
    read -p "Zone ID: " ZONE_ID
    read -p "DNS Record Name (e.g., home.example.com): " RECORD_NAME
    
    # Create config directory
    mkdir -p "$INSTALL_DIR/config"
    
    # Create configuration file
    cat > "$INSTALL_DIR/config/config.yml" << EOF
cloudflare:
  apiToken: "$CF_TOKEN"
  zoneId: "$ZONE_ID"
  recordName: "$RECORD_NAME"
  recordType: "A"

monitoring:
  checkInterval: 300
  retryInterval: 60
  maxRetries: 3

logging:
  level: "INFO"
  file: "logs/dns-updater.log"
  maxSize: "10MB"
  maxBackups: 5
EOF
}

# Check Java installation
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo -e "${YELLOW}Java not found. Installing OpenJDK 17...${NC}"
    apt-get update
    apt-get install -y openjdk-17-jre-headless
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to install Java${NC}"
        exit 1
    fi
fi

# Create installation directory
echo "Creating installation directory..."
mkdir -p "$INSTALL_DIR/logs"

# Download JAR file
echo "Downloading DNS Updater..."
if [ -f "dns-updater.jar" ]; then
    cp dns-updater.jar "$INSTALL_DIR/dns-updater.jar"
else
    echo -e "${RED}dns-updater.jar not found in current directory${NC}"
    exit 1
fi

# Configure the application
configure_dns_updater

# Create systemd service
echo "Creating systemd service..."
cat > "/etc/systemd/system/$SERVICE_NAME.service" << EOF
[Unit]
Description=Dynamic DNS Updater Service
After=network.target

[Service]
Type=simple
User=nobody
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/bin/java -jar $INSTALL_DIR/dns-updater.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Set permissions
echo "Setting permissions..."
chown -R nobody:nogroup "$INSTALL_DIR"
chmod 640 "$INSTALL_DIR/config/config.yml"
chmod 755 "$INSTALL_DIR/dns-updater.jar"

# Enable and start service
echo "Starting service..."
systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
systemctl start "$SERVICE_NAME"

# Check service status
if systemctl is-active --quiet "$SERVICE_NAME"; then
    echo -e "${GREEN}Installation successful!${NC}"
    echo -e "Service is running. Check status with: ${YELLOW}systemctl status $SERVICE_NAME${NC}"
    echo -e "View logs with: ${YELLOW}journalctl -u $SERVICE_NAME -f${NC}"
    echo -e "Application logs: ${YELLOW}tail -f $INSTALL_DIR/logs/dns-updater.log${NC}"
else
    echo -e "${RED}Service failed to start. Please check logs:${NC}"
    echo -e "${YELLOW}systemctl status $SERVICE_NAME${NC}"
    exit 1
fi 