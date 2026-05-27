# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Manage **multiple DNS records across multiple zones/domains** from one config (`cloudflare.records` list), with optional per-record API token override
- Records that don't exist yet are **created automatically** (TTL automatic, unproxied) instead of failing
- IPv6 (`AAAA`) record support and IPv4/IPv6-aware IP lookup
- Docker support: multi-stage `Dockerfile`, `docker-compose.yml`, `.dockerignore`
- `CONFIG_PATH` environment variable to set the config location

### Changed
- IP lookups now use HTTPS providers only (ipify + icanhazip); the plaintext-HTTP ip-api provider was removed
- A single shared HTTP client is reused across all lookups, Cloudflare calls, and notifications
- The legacy single-record config format is still accepted for backwards compatibility

### Fixed
- Retry logic (`retryInterval`/`maxRetries`) is now actually implemented as documented
- Restarts no longer trigger spurious DNS updates or notifications — Cloudflare's live record is the source of truth
- The `logging` config (level/file/size/backups) is now applied to logback instead of being ignored
- Telegram payloads are built with a JSON serializer, removing a JSON-injection risk
- Cloudflare URL parameters are now URL-encoded
- Consistent `clouddnsync` naming across docs, installer, service, and log files
- `install.sh`: removed a duplicated start block and replaced fragile config parsing

## [1.0.0] - 2024-02-17
### Added
- IP change detection to prevent unnecessary updates
- Optional Telegram notifications when IP changes
- Initial release
- Automatic IP address monitoring
- Cloudflare DNS integration
- Systemd service support
- YAML configuration
- Logging with rotation
- Installation script for Linux systems 