# Installation directory
INSTALL_DIR="/opt/clouddnsync"
SERVICE_NAME="clouddnsync"
JAR_URL="https://github.com/kibukamusoke/cloudflare-dns-sync/releases/latest/download/clouddnsync.jar"

# Download JAR file
echo "Downloading DNS Updater..."
if [ -f "clouddnsync.jar" ]; then
    cp clouddnsync.jar "$INSTALL_DIR/clouddnsync.jar"
else
    echo -e "${RED}clouddnsync.jar not found in current directory${NC}"
    exit 1
fi

# Update ExecStart line in systemd service
ExecStart=/usr/bin/java -jar $INSTALL_DIR/clouddnsync.jar 