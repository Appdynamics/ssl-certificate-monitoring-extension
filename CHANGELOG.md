# AppDynamics Extension SSL Certificate Monitor CHANGELOG

### Version 3.0.3
- Added optional `targetHost` field per domain to support monitoring backend certificates behind a load balancer (APPDSIM-1)
- Added `-servername` flag to the openssl script invocations so the SNI hostname is always sent, fixing wrong certificates returned by openssl builds that do not send SNI by default (e.g. LibreSSL)
- The SNI hostname is not sent when `domain` is an IP address (IPv4/IPv6), preserving the previous behaviour for such entries
- `openssl.sh` / `openssl.bat` now take `<connectTo> <port> [sniHostname]`; SNI is sent only when the third argument is given
- Updated appd-exts-commons to 2.2.21

### Version 3.0.2
- Removed unwanted dependencies

### Version 3.0.1
- Revamped extension to appd-exts-commons 2.2.4