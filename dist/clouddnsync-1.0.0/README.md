Dynamic DNS Updater for Cloudflare
=================================

Automatically update your Cloudflare DNS records when your IP address changes. Perfect for home servers, remote access, and dynamic IP setups.

Features
--------
* Automatic IP address monitoring
* Cloudflare DNS integration
* Runs as a system service
* Configurable check intervals
* Detailed logging
* Supports Linux systems (Windows support coming soon)

Prerequisites
------------
Linux:
- Ubuntu/Debian-based system
- Java 17 or later (automatically installed if missing)
- Root/sudo access
- Internet connection
- Cloudflare account with:
  * API Token with Zone:DNS:Edit permissions
  * Zone ID
  * Domain configured in Cloudflare
- (Optional) Telegram bot for notifications:
  * Telegram account
  * Bot token from @BotFather
  * Chat ID where notifications will be sent

Quick Start
----------
1. Installation:
   # Option 1: Download release (Recommended)
   $ wget https://github.com/kibukamusoke/cloudflare-dns-sync/releases/download/v1.0.0/clouddnsync-1.0.0.tar.gz
   $ tar -xzf clouddnsync-1.0.0.tar.gz
   $ cd clouddnsync-1.0.0
   $ sudo ./install.sh

   # Option 2: Build from source
   $ git clone https://github.com/kibukamusoke/cloudflare-dns-sync
   $ cd cloudflare-dns-sync
   $ mvn clean package
   $ sudo ./install.sh

2. Create installation directory:
   $ mkdir dns-updater-install
   $ cp target/dynamic-dns-updater-1.0-SNAPSHOT-jar-with-dependencies.jar dns-updater-install/dns-updater.jar
   $ cp install-dns-updater.sh dns-updater-install/
   $ chmod +x dns-updater-install/install-dns-updater.sh

3. Run the installation script:
   $ sudo ./install-dns-updater.sh

4. Follow the prompts to enter your Cloudflare configuration:
   - API Token (from Cloudflare dashboard)
   - Zone ID (from Cloudflare domain overview)
   - DNS Record Name (e.g., home.example.com)

Configuration
------------
The configuration file is located at /opt/dns-updater/config/config.yml:

cloudflare:
  apiToken: "your-api-token"
  zoneId: "your-zone-id"
  recordName: "your.domain.com"
  recordType: "A"

monitoring:
  # How often to check for IP changes
  checkInterval: 300  # Check every 5 minutes
  
  # How long to wait before retrying after a failure
  retryInterval: 60   # Wait 60 seconds between retry attempts
  
  # Maximum number of retries per attempt
  maxRetries: 3      # Try up to 3 times if an attempt fails

# Example retry scenario:
# 1. IP check fails
# 2. Wait 60 seconds (retryInterval)
# 3. Try again, fails again
# 4. Wait 60 seconds
# 5. Try one last time
# 6. If still failing, wait 5 minutes (checkInterval) and start over

notifications:
  telegram:
    enabled: false    # Set to true to enable Telegram notifications
    botToken: "your-bot-token"
    chatId: "your-chat-id"
    message: "IP address changed to: {ip}"

logging:
  level: "INFO"
  file: "logs/dns-updater.log"
  maxSize: "10MB"
  maxBackups: 5

Telegram Setup
-------------
1. Create a Telegram Bot:
   * Open Telegram and search for @BotFather
   * Send /newbot command
   * Follow instructions to create your bot
   * Choose a name for your bot (e.g., "My Home DNS Bot")
   * Choose a username for your bot (must end in 'bot', e.g., "my_home_dns_bot")
   * Save the HTTP API token provided (this is your botToken)
   * The token looks like: "1234567890:ABCdefGHIjklMNOpqrsTUVwxyz"

2. Get your Chat ID:
   * Start a chat with your new bot
   * Click the "Start" button or send "/start"
   * Send any message to the bot
   * Visit: https://api.telegram.org/bot<YOUR-BOT-TOKEN>/getUpdates
   * Replace <YOUR-BOT-TOKEN> with the token from step 1
   * Look for "chat":{"id":NUMBERS} in the response
   * The chat ID will be a number like "123456789" (can be negative)
   * Save this number (this is your chatId)

3. Enable notifications:
   * Edit /opt/dns-updater/config/config.yml
   * Set notifications.telegram.enabled to true
   * Add your botToken and chatId
   * Customize the message if desired
   * Message can include {ip} placeholder which will be replaced with the new IP
   * Restart the service: sudo systemctl restart dns-updater

Example configuration:
```yaml
notifications:
  telegram:
    enabled: true
    botToken: "1234567890:ABCdefGHIjklMNOpqrsTUVwxyz"
    chatId: "123456789"
    message: "🏠 Home IP changed to: {ip}"
```

Telegram Troubleshooting
----------------------
1. Bot not responding:
   - Verify bot token is correct
   - Make sure you started a chat with the bot
   - Check if bot appears online in Telegram

2. Not receiving notifications:
   - Verify chatId is correct
   - Check if notifications.telegram.enabled is true
   - Look for Telegram-related errors in logs:
     $ sudo tail -f /opt/dns-updater/logs/dns-updater.log | grep -i telegram

3. API errors:
   - Test bot token: Visit https://api.telegram.org/bot<YOUR-BOT-TOKEN>/getMe
   - Should return {"ok":true, ...}
   - If not, token is invalid or bot was deleted

4. Message formatting:
   - Basic text and emojis are supported
   - Avoid special characters in message template
   - Test message format in Telegram before using

Service Management
----------------
Check Status:
$ sudo systemctl status dns-updater

Start/Stop/Restart:
$ sudo systemctl start dns-updater    # Start service
$ sudo systemctl stop dns-updater     # Stop service
$ sudo systemctl restart dns-updater  # Restart service

View Logs:
$ sudo journalctl -u dns-updater -f           # View service logs
$ sudo tail -f /opt/dns-updater/logs/dns-updater.log  # View application logs

Uninstallation
-------------
To remove the service and all its files:
$ sudo systemctl stop dns-updater
$ sudo systemctl disable dns-updater
$ sudo rm /etc/systemd/system/dns-updater.service
$ sudo rm -rf /opt/dns-updater
$ sudo systemctl daemon-reload

Troubleshooting
--------------
1. Service won't start:
   - Check logs: sudo journalctl -u dns-updater -f
   - Verify Java installation: java -version
   - Ensure config.yml has correct permissions
   - Verify Cloudflare credentials

2. DNS not updating:
   - Check network connectivity
   - Verify API token permissions
   - Look for errors in /opt/dns-updater/logs/dns-updater.log
   - Note: On network errors, the application will:
     * Retry up to 3 times with 60-second intervals
     * If all retries fail, wait 5 minutes and try again
     * The service will keep running even during network outages

3. Permission issues:
   - Check file ownership: ls -l /opt/dns-updater
   - Ensure service user has access: sudo chown -R nobody:nogroup /opt/dns-updater

Contributing
-----------
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

License
-------
This project is licensed under the MIT License - see the LICENSE file for details.

Acknowledgments
--------------
- Uses Cloudflare API for DNS management
- Built with Java 17
- Uses Maven for dependency management 