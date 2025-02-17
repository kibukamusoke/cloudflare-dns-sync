# Installation directory
INSTALL_DIR="/opt/clouddnsync"
SERVICE_NAME="clouddnsync"
JAR_URL="https://github.com/kibukamusoke/cloudflare-dns-sync/releases/download/v1.0.0/clouddnsync.jar"

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
  file: "logs/clouddnsync.log"
  maxSize: "10MB"
  maxBackups: 5
EOF
}

# Create installation directory
echo "Creating installation directory..."
mkdir -p "$INSTALL_DIR/logs"

# Download JAR file
echo "Downloading CloudDNSync..."
if [ -f "clouddnsync.jar" ]; then
    cp clouddnsync.jar "$INSTALL_DIR/clouddnsync.jar"
else
    echo "Downloading from GitHub releases..."
    wget -O "$INSTALL_DIR/clouddnsync.jar" "$JAR_URL"
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to download clouddnsync.jar${NC}"
        exit 1
    fi
fi

# Create systemd service
echo "Creating systemd service..."
cat > "/etc/systemd/system/$SERVICE_NAME.service" << EOF
[Unit]
Description=CloudDNSync - Cloudflare Dynamic DNS Updater
After=network.target

[Service]
Type=simple
User=nobody
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/bin/java -jar $INSTALL_DIR/clouddnsync.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Set permissions
echo "Setting permissions..."
chown -R nobody:nogroup "$INSTALL_DIR"
chmod 640 "$INSTALL_DIR/config/config.yml"
chmod 755 "$INSTALL_DIR/clouddnsync.jar"

# Configure the application
configure_dns_updater

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
    echo -e "Application logs: ${YELLOW}tail -f $INSTALL_DIR/logs/clouddnsync.log${NC}"
else
    echo -e "${RED}Service failed to start. Please check logs:${NC}"
    echo -e "${YELLOW}systemctl status $SERVICE_NAME${NC}"
    exit 1
fi 