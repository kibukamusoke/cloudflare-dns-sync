#!/bin/bash
set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Installation settings
INSTALL_DIR="/opt/clouddnsync"
SERVICE_NAME="clouddnsync"
JAR_URL="https://github.com/kibukamusoke/cloudflare-dns-sync/releases/download/v1.0.0/clouddnsync.jar"
LOCAL_JAR_CANDIDATES=("clouddnsync.jar" "target/clouddnsync-1.0.0-jar-with-dependencies.jar")
CONFIG_FILE="$INSTALL_DIR/config/config.yml"

# Helpers to read existing config values robustly (first match wins).
# extract_quoted KEY -> value inside the first "..." on a line containing KEY:
extract_quoted() {
    grep -E "^[[:space:]]*$1:" "$CONFIG_FILE" 2>/dev/null | head -1 | sed -E 's/.*"([^"]*)".*/\1/'
}
# extract_bool KEY -> the bare token after KEY: (e.g. enabled: true)
extract_bool() {
    grep -E "^[[:space:]]*$1:" "$CONFIG_FILE" 2>/dev/null | head -1 | sed -E 's/.*:[[:space:]]*([^"[:space:]]+).*/\1/'
}

# Check and install Java if needed
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo -e "${YELLOW}Java not found. Installing OpenJDK 17...${NC}"
    apt-get update
    if ! apt-get install -y openjdk-17-jre-headless; then
        echo -e "${RED}Failed to install Java${NC}"
        exit 1
    fi
fi

# Check if this is an update
IS_UPDATE=false
if [ -f "$CONFIG_FILE" ]; then
    IS_UPDATE=true
    echo -e "${YELLOW}Existing installation detected. Current values will be shown in brackets.${NC}"
    echo -e "${YELLOW}Press Enter to keep the current value, or type a new value.${NC}\n"
    cp "$CONFIG_FILE" "$CONFIG_FILE.backup"
fi

echo -e "${GREEN}CloudDNSync Installation Script${NC}"
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
    TELEGRAM_MESSAGE="🏠 IP address for {record} changed to: {ip}"

    # Read existing configuration if available
    if [ "$IS_UPDATE" = true ]; then
        CF_TOKEN=$(extract_quoted "apiToken")
        ZONE_ID=$(extract_quoted "zoneId")
        RECORD_NAME=$(extract_quoted "recordName")
        TELEGRAM_ENABLED=$(extract_bool "enabled")
        TELEGRAM_TOKEN=$(extract_quoted "botToken")
        TELEGRAM_CHAT_ID=$(extract_quoted "chatId")
        TELEGRAM_MESSAGE=$(extract_quoted "message")
    fi

    echo -e "${YELLOW}Please enter your Cloudflare configuration:${NC}"
    echo -e "${YELLOW}(This sets up one record; add more later by editing $CONFIG_FILE)${NC}"
    read -p "Cloudflare API Token [$CF_TOKEN]: " NEW_TOKEN
    read -p "Zone ID [$ZONE_ID]: " NEW_ZONE_ID
    read -p "DNS Record Name [$RECORD_NAME]: " NEW_RECORD_NAME

    # Use new values if provided, otherwise keep existing
    CF_TOKEN=${NEW_TOKEN:-$CF_TOKEN}
    ZONE_ID=${NEW_ZONE_ID:-$ZONE_ID}
    RECORD_NAME=${NEW_RECORD_NAME:-$RECORD_NAME}

    echo -e "\n${YELLOW}Telegram configuration (optional):${NC}"
    read -p "Bot Token [$TELEGRAM_TOKEN]: " NEW_BOT_TOKEN
    read -p "Chat ID [$TELEGRAM_CHAT_ID]: " NEW_CHAT_ID
    read -p "Notification message [$TELEGRAM_MESSAGE]: " NEW_MSG

    TELEGRAM_TOKEN=${NEW_BOT_TOKEN:-$TELEGRAM_TOKEN}
    TELEGRAM_CHAT_ID=${NEW_CHAT_ID:-$TELEGRAM_CHAT_ID}
    TELEGRAM_MESSAGE=${NEW_MSG:-$TELEGRAM_MESSAGE}

    # Enable Telegram only when both token and chat id are present
    if [ -z "$TELEGRAM_TOKEN" ] || [ -z "$TELEGRAM_CHAT_ID" ]; then
        TELEGRAM_ENABLED="false"
        echo -e "\n${YELLOW}Telegram notifications disabled. Missing required settings:${NC}"
        [ -z "$TELEGRAM_TOKEN" ] && echo -e "${YELLOW}- Bot token not provided${NC}"
        [ -z "$TELEGRAM_CHAT_ID" ] && echo -e "${YELLOW}- Chat ID not provided${NC}"
        echo -e "${YELLOW}You can enable Telegram later by editing $CONFIG_FILE${NC}"
    else
        TELEGRAM_ENABLED="true"
        echo -e "\n${GREEN}Telegram notifications enabled${NC}"
        echo -e "- Bot token: ${TELEGRAM_TOKEN:0:5}...${TELEGRAM_TOKEN: -5}"
        echo -e "- Chat ID: $TELEGRAM_CHAT_ID"
        echo -e "- Message template: $TELEGRAM_MESSAGE"

        echo -e "\n${YELLOW}Sending test message...${NC}"
        TEST_URL="https://api.telegram.org/bot$TELEGRAM_TOKEN/sendMessage"
        set +e
        RESPONSE=$(curl -s --connect-timeout 5 --max-time 10 -X POST "$TEST_URL" \
            --data-urlencode "chat_id=$TELEGRAM_CHAT_ID" \
            --data-urlencode "text=CloudDNSync installation test message")
        CURL_EXIT=$?
        set -e

        if [ $CURL_EXIT -eq 28 ]; then
            echo -e "${YELLOW}Request timed out. Proceeding with installation.${NC}"
        elif [ $CURL_EXIT -eq 0 ] && echo "$RESPONSE" | grep -q '"ok":true'; then
            echo -e "${GREEN}Test message sent successfully! Check your Telegram.${NC}"
        else
            echo -e "${RED}Failed to send test message. Error code: $CURL_EXIT${NC}"
            [ -n "$RESPONSE" ] && echo -e "${YELLOW}Response: $RESPONSE${NC}"
            echo -e "${YELLOW}Config will be saved; you may need to troubleshoot Telegram settings.${NC}"
        fi
    fi

    # Create config directory and file (new multi-record format)
    mkdir -p "$INSTALL_DIR/config"
    cat > "$CONFIG_FILE" << EOF
cloudflare:
  apiToken: "$CF_TOKEN"
  records:
    - zoneId: "$ZONE_ID"
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
}

# Create installation directory
echo "Creating installation directory..."
mkdir -p "$INSTALL_DIR/logs"

# Obtain the JAR: prefer a local build, otherwise download the release
echo "Locating CloudDNSync jar..."
JAR_FOUND=""
for candidate in "${LOCAL_JAR_CANDIDATES[@]}"; do
    if [ -f "$candidate" ]; then
        JAR_FOUND="$candidate"
        break
    fi
done

if [ -n "$JAR_FOUND" ]; then
    echo "Using local jar: $JAR_FOUND"
    cp "$JAR_FOUND" "$INSTALL_DIR/clouddnsync.jar"
else
    echo "Downloading from GitHub releases..."
    if ! wget -O "$INSTALL_DIR/clouddnsync.jar" "$JAR_URL"; then
        echo -e "${RED}Failed to download clouddnsync.jar${NC}"
        exit 1
    fi
fi

# Configure the application
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
# Give the app time to shut down gracefully
TimeoutStopSec=10
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Set permissions
echo "Setting permissions..."
chown -R nobody:nogroup "$INSTALL_DIR"
chmod 640 "$CONFIG_FILE"
chmod 755 "$INSTALL_DIR/clouddnsync.jar"

# Verify the jar can run before enabling the service
echo "Verifying Java can execute the jar..."
if ! java -jar "$INSTALL_DIR/clouddnsync.jar" --version &> /dev/null; then
    echo -e "${RED}Failed to execute jar file. Check Java installation.${NC}"
    exit 1
fi

# Enable and start the service
echo "Starting service..."
systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
systemctl restart "$SERVICE_NAME"

# Verify the service is enabled for auto-start
if ! systemctl is-enabled --quiet "$SERVICE_NAME"; then
    echo -e "${RED}Warning: Failed to enable service for auto-start${NC}"
    echo -e "Try manually: ${YELLOW}sudo systemctl enable $SERVICE_NAME${NC}"
else
    echo -e "${GREEN}Service enabled for auto-start on boot${NC}"
fi

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
