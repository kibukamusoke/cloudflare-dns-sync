# CloudDNSync — Dynamic DNS Updater for Cloudflare

Automatically keep your Cloudflare DNS records pointed at your current public IP. Perfect for home servers, remote access, and any setup with a dynamic IP.

## Features

- Automatic public-IP monitoring with provider fallback (ipify → icanhazip, all over HTTPS)
- **Multiple records across multiple zones / domains** from a single instance
- IPv4 (`A`) and IPv6 (`AAAA`) records
- Cloudflare is treated as the source of truth, so restarts never cause spurious updates or notifications
- Configurable retry behaviour on lookup failures
- Optional Telegram notifications when a record changes
- Runs as a **systemd service** or in **Docker**
- Rotating file logs

## Requirements

- A Cloudflare account with:
  - An API token with `Zone:DNS:Edit` permission (one token can cover several zones)
  - The Zone ID of each domain you want to update (Domain → Overview in the dashboard)
- For the systemd install: an Ubuntu/Debian host with root access (Java 17 is installed automatically if missing)
- For Docker: Docker and Docker Compose

## Configuration

CloudDNSync reads a single YAML file. The default location is `config/config.yml` (relative to the working directory); override it with the `CONFIG_PATH` env var or by passing the path as the first argument.

```yaml
cloudflare:
  # Account-wide token, used for every record unless one overrides it.
  apiToken: "your-cloudflare-api-token"

  # One or more records, in any zone / any domain.
  records:
    - zoneId: "zone-id-for-example-com"
      recordName: "home.example.com"
      recordType: "A"

    - zoneId: "zone-id-for-example-org"
      recordName: "vpn.example.org"
      recordType: "A"

    - zoneId: "zone-id-for-example-net"
      recordName: "ipv6.example.net"
      recordType: "AAAA"
      apiToken: "optional-per-record-token"   # overrides cloudflare.apiToken

monitoring:
  checkInterval: 300   # how often to check, in seconds (default 300 = 5 min)
  retryInterval: 60    # seconds to wait between failed lookup attempts
  maxRetries: 3        # lookup attempts per check before waiting for the next interval

notifications:
  telegram:
    enabled: false
    botToken: "your-bot-token"
    chatId: "your-chat-id"
    message: "IP address for {record} changed to: {ip}"   # supports {ip} and {record}

logging:
  level: "INFO"                  # DEBUG, INFO, WARN, ERROR
  file: "logs/clouddnsync.log"
  maxSize: "10MB"
  maxBackups: 5
```

> **Backwards compatibility:** the older single-record format (`cloudflare.zoneId` /
> `recordName` / `recordType` at the top level, without a `records:` list) is still
> accepted and treated as one record.

### How updates work

On each interval the current public IP is looked up once per needed version (IPv4/IPv6). For every record, CloudDNSync compares against the **live Cloudflare record content** and only issues an update — and only sends a notification — when it actually differs. If a record does not exist yet, it is **created automatically** (TTL automatic, unproxied). If a lookup fails it retries up to `maxRetries` times, `retryInterval` seconds apart; if all attempts fail it logs the error and waits for the next interval. The service keeps running through network outages.

## Option 1 — Docker (recommended)

```bash
git clone https://github.com/kibukamusoke/cloudflare-dns-sync
cd cloudflare-dns-sync

# Create your config from the example and edit it
cp config/config.yml.example config/config.yml
$EDITOR config/config.yml

# Build and run
docker compose up -d
```

- Your config is mounted read-only from `./config`; logs are written to `./logs`.
- View logs: `docker compose logs -f`
- Restart after editing config: `docker compose restart`
- Stop: `docker compose down`

> Managing `AAAA` records requires the container to reach the IPv6 internet.
> Uncomment `network_mode: host` in `docker-compose.yml` (and ensure your host
> has IPv6) if you use them.

## Option 2 — systemd (Linux)

Build from source (or download the release jar) and run the installer:

```bash
git clone https://github.com/kibukamusoke/cloudflare-dns-sync
cd cloudflare-dns-sync
mvn clean package
sudo ./install.sh
```

The installer:
- installs to `/opt/clouddnsync`,
- prompts for one record plus optional Telegram settings (add more records afterwards by editing `/opt/clouddnsync/config/config.yml`),
- creates and starts a `clouddnsync` systemd service.

It prefers a locally built jar (`target/clouddnsync-1.0.0-jar-with-dependencies.jar` or `./clouddnsync.jar`) and falls back to downloading the release.

### Service management

```bash
sudo systemctl status clouddnsync     # status
sudo systemctl restart clouddnsync    # restart (e.g. after editing config)
sudo systemctl stop clouddnsync       # stop
sudo journalctl -u clouddnsync -f     # service logs
sudo tail -f /opt/clouddnsync/logs/clouddnsync.log   # application logs
```

### Uninstall

```bash
sudo systemctl stop clouddnsync
sudo systemctl disable clouddnsync
sudo rm /etc/systemd/system/clouddnsync.service
sudo rm -rf /opt/clouddnsync
sudo systemctl daemon-reload
```

## Telegram notifications

1. Create a bot with [@BotFather](https://t.me/BotFather) (`/newbot`) and copy the HTTP API token.
2. Start a chat with your bot, send it a message, then visit
   `https://api.telegram.org/bot<YOUR-BOT-TOKEN>/getUpdates` and read the
   `chat.id` value.
3. Set `notifications.telegram.enabled: true` and fill in `botToken` and `chatId`.
4. The `message` template supports `{ip}` (the new address) and `{record}` (the record name).

Restart the service/container after editing the config.

## Running standalone

```bash
java -jar target/clouddnsync-1.0.0-jar-with-dependencies.jar [path/to/config.yml]
java -jar target/clouddnsync-1.0.0-jar-with-dependencies.jar --version
java -jar target/clouddnsync-1.0.0-jar-with-dependencies.jar --debug    # force DEBUG logging
```

## Troubleshooting

- **Service won't start** — check `sudo journalctl -u clouddnsync -f`; verify `java -version` and that the config validates (a bad config prints the reason to stderr/journal on startup).
- **DNS not updating** — confirm the API token has `Zone:DNS:Edit` on the right zone and the `zoneId`/`recordName` are correct. If the record doesn't exist, CloudDNSync creates it automatically; a failure to create usually means the token lacks edit permission on that zone. Check the logs.
- **No Telegram messages** — verify `botToken`/`chatId`, that `enabled: true`, and look for `telegram` errors in the logs. Test the token at `https://api.telegram.org/bot<TOKEN>/getMe`.

## License

MIT — see [LICENSE](LICENSE).

## Acknowledgments

- Uses the Cloudflare API for DNS management
- Built with Java 17 and Maven
