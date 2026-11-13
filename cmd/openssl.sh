#! /bin/sh

TARGET_HOST=$1
PORT=$2

# OPENSSL_BIN can be set to use a different openssl binary.
OPENSSL=${OPENSSL_BIN:-/usr/bin/openssl}

SNI_OPT=""
if [ -n "$3" ]; then
    SNI_OPT="-servername $3"
elif $OPENSSL s_client -help 2>&1 | grep -q -- -noservername; then
    # OpenSSL 1.1.1+ derives SNI from the -connect hostname unless told not to.
    # LibreSSL has no such default (and no -noservername), so the flag is only added when supported.
    SNI_OPT="-noservername"
fi

echo -e “quit\n” | $OPENSSL s_client -connect ${TARGET_HOST}:${PORT} ${SNI_OPT} 2>/dev/null | $OPENSSL x509 -noout -enddate