#! /bin/sh

echo -e “quit\n” | /usr/bin/openssl s_client -connect $1:$2 2>/dev/null | /usr/bin/openssl x509 -noout -enddate