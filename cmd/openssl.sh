#! /bin/sh

TARGET_HOST=$1
PORT=$2

SNI_OPT=""
if [ -n "$3" ]; then
    SNI_OPT="-servername $3"
fi

echo -e “quit\n” | /usr/bin/openssl s_client -connect ${TARGET_HOST}:${PORT} ${SNI_OPT} 2>/dev/null | /usr/bin/openssl x509 -noout -enddate