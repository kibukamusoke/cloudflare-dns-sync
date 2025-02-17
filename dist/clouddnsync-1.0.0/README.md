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

Quick Start
----------
1. Build the project (for developers):
   $ git clone https://github.com/kibukamusoke/dns-updater
   $ cd dns-updater
   $ mvn clean package

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
  checkInterval: 300  # seconds
  retryInterval: 60   # seconds
  maxRetries: 3

logging:
  level: "INFO"
  file: "logs/dns-updater.log"
  maxSize: "10MB"
  maxBackups: 5

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