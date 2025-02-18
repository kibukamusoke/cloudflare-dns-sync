#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check and install Java if needed
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

# Installation directory
INSTALL_DIR="/opt/clouddnsync"
SERVICE_NAME="clouddnsync"
JAR_URL="https://github.com/kibukamusoke/cloudflare-dns-sync/releases/download/v1.0.0/clouddnsync.jar"

# Check if this is an update
IS_UPDATE=false
if [ -f "$INSTALL_DIR/config/config.yml" ]; then
    IS_UPDATE=true
    echo -e "${YELLOW}Existing installation detected.${NC}"
    read -p "Would you like to keep the existing configuration? (Y/n): " KEEP_CONFIG
    if [[ "$KEEP_CONFIG" =~ ^[Nn]$ ]]; then
        IS_UPDATE=false
        echo -e "${YELLOW}Backing up old configuration to config.yml.backup...${NC}"
        cp "$INSTALL_DIR/config/config.yml" "$INSTALL_DIR/config/config.yml.backup"
    else
        echo -e "${GREEN}Keeping existing configuration.${NC}"
    fi
fi

echo -e "${GREEN}Dynamic DNS Updater Installation Script${NC}"
echo "----------------------------------------"

# Function to prompt for configuration values
configure_dns_updater() {
    # Default values
    CF_TOKEN=""
    ZONE_ID=""
    RECORD_NAME=""
    TELEGRAM_ENABLED="false"
    TELEGRAM_TOKEN=""
    TELEGRAM_CHAT_ID=""
    TELEGRAM_MESSAGE="🏠 Home IP changed to: {ip}"

    # Read existing configuration if available
    if [ "$IS_UPDATE" = true ]; then
        echo -e "${YELLOW}Current configuration values will be shown in brackets.${NC}"
        echo -e "${YELLOW}Press Enter to keep the current value, or type a new value.${NC}\n"
        
        # Extract current values using grep and cut
        CF_TOKEN=$(grep "apiToken:" "$INSTALL_DIR/config/config.yml" | cut -d'"' -f2)
        ZONE_ID=$(grep "zoneId:" "$INSTALL_DIR/config/config.yml" | cut -d'"' -f2)
        RECORD_NAME=$(grep "recordName:" "$INSTALL_DIR/config/config.yml" | cut -d'"' -f2)
        TELEGRAM_ENABLED=$(grep "enabled:" "$INSTALL_DIR/config/config.yml" | cut -d' ' -f6)
        TELEGRAM_TOKEN=$(grep "botToken:" "$INSTALL_DIR/config/config.yml" | cut -d'"' -f2)
        TELEGRAM_CHAT_ID=$(grep "chatId:" "$INSTALL_DIR/config/config.yml" | cut -d'"' -f2)
        TELEGRAM_MESSAGE=$(grep "message:" "$INSTALL_DIR/config/config.yml" | cut -d'"' -f2)
    fi

    echo -e "${YELLOW}Please enter your Cloudflare configuration:${NC}"
    read -p "Cloudflare API Token [$CF_TOKEN]: " NEW_TOKEN
    read -p "Zone ID [$ZONE_ID]: " NEW_ZONE_ID
    read -p "DNS Record Name [$RECORD_NAME]: " NEW_RECORD_NAME
    
    # Use new values if provided, otherwise keep existing
    CF_TOKEN=${NEW_TOKEN:-$CF_TOKEN}
    ZONE_ID=${NEW_ZONE_ID:-$ZONE_ID}
    RECORD_NAME=${NEW_RECORD_NAME:-$RECORD_NAME}
    
    if [ "$TELEGRAM_ENABLED" = "true" ]; then
        echo -e "\n${YELLOW}Telegram notifications are currently enabled.${NC}"
        read -p "Would you like to modify Telegram settings? (y/N): " SETUP_TELEGRAM
    else
        echo -e "\n${YELLOW}Would you like to set up Telegram notifications? (y/N):${NC}"
        read -p "" SETUP_TELEGRAM
    fi
    
    if [[ "$SETUP_TELEGRAM" =~ ^[Yy]$ ]]; then
        TELEGRAM_ENABLED="true"
        echo -e "\n${YELLOW}Please enter your Telegram configuration:${NC}"
        read -p "Bot Token [$TELEGRAM_TOKEN]: " NEW_BOT_TOKEN
        read -p "Chat ID [$TELEGRAM_CHAT_ID]: " NEW_CHAT_ID
        read -p "Notification message [$TELEGRAM_MESSAGE]: " NEW_MSG

        # Use new values if provided, otherwise keep existing
        TELEGRAM_TOKEN=${NEW_BOT_TOKEN:-$TELEGRAM_TOKEN}
        TELEGRAM_CHAT_ID=${NEW_CHAT_ID:-$TELEGRAM_CHAT_ID}
        TELEGRAM_MESSAGE=${NEW_MSG:-$TELEGRAM_MESSAGE}
    fi
    
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

notifications:
  telegram:
    enabled: $TELEGRAM_ENABLED
    botToken: "$TELEGRAM_TOKEN"
    chatId: "$TELEGRAM_CHAT_ID"
    message: "$TELEGRAM_MESSAGE"

logging:
  level: "INFO"
  file: "logs/clouddnsync.log"
  maxSize: "10MB"
  maxBackups: 5
EOF

    if [[ "$SETUP_TELEGRAM" =~ ^[Yy]$ ]]; then
        echo -e "\n${YELLOW}Testing Telegram configuration...${NC}"
        echo "Attempting to send test message..."
        TEST_URL="https://api.telegram.org/bot$TELEGRAM_TOKEN/sendMessage"
        TEST_MSG="CloudDNSync installation test message"
        
        # Debug output
        echo "Debug: Using bot token: ${TELEGRAM_TOKEN:0:5}...${TELEGRAM_TOKEN: -5}"
        echo "Debug: Using chat ID: $TELEGRAM_CHAT_ID"
        echo "Debug: Test URL: $TEST_URL"
        echo "Debug: Test message: $TEST_MSG"
        
        # Properly encode the JSON payload
        JSON_DATA="{\"chat_id\":\"$TELEGRAM_CHAT_ID\",\"text\":\"$TEST_MSG\"}"
        RESPONSE=$(curl -s \
            --connect-timeout 10 \
            --max-time 10 \
            -X POST \
            -H "Content-Type: application/json" \
            -d "$JSON_DATA" \
            "$TEST_URL")
        CURL_EXIT=$?
        
        # Debug curl result
        echo "Debug: curl exit code: $CURL_EXIT"
        echo "Debug: JSON payload: $JSON_DATA"
        echo "Debug: Raw response: $RESPONSE"
        
        if echo "$RESPONSE" | grep -q '"ok":true'; then
            echo -e "${GREEN}Telegram configuration successful! Check your Telegram for a test message.${NC}"
        else
            echo -e "${RED}Failed to send Telegram test message.${NC}"
            if [ -z "$RESPONSE" ]; then
                echo -e "${YELLOW}No response received from Telegram API${NC}"
            else
                echo -e "${YELLOW}API Response: $RESPONSE${NC}"
            fi

            # Check common issues
            if [[ "$RESPONSE" == *"Unauthorized"* ]]; then
                echo "Error: Invalid bot token"
            elif [[ "$RESPONSE" == *"Bad Request: chat not found"* ]]; then
                echo "Error: Invalid chat ID"
            elif [[ "$RESPONSE" == *"Bad Request: message text is empty"* ]]; then
                echo "Error: Empty message text"
            fi

            echo "You can update these settings later in $INSTALL_DIR/config/config.yml"
        fi
    fi
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

# Configure the application first
configure_dns_updater

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
# Give the app time to shutdown gracefully
TimeoutStopSec=10
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Set permissions
echo "Setting permissions..."
mkdir -p /var/log
touch /var/log/clouddnsync.log
chown -R nobody:nogroup "$INSTALL_DIR"
chown nobody:nogroup /var/log/clouddnsync.log
chmod 640 "$INSTALL_DIR/config/config.yml"
chmod 755 "$INSTALL_DIR/clouddnsync.jar"
chmod 644 /var/log/clouddnsync.log

# Enable and start service
echo "Starting service..."
if [ ! -f "$INSTALL_DIR/clouddnsync.jar" ]; then
    echo -e "${RED}JAR file not found at $INSTALL_DIR/clouddnsync.jar${NC}"
    exit 1
fi

echo "Verifying Java can execute the JAR..."
if ! java -jar "$INSTALL_DIR/clouddnsync.jar" --version &> /dev/null; then
    echo -e "${RED}Failed to execute JAR file. Check Java installation.${NC}"
    exit 1
fi

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