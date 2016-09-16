@echo off

echo | openssl s_client -connect %1:%2 2> null | openssl x509 -noout -enddate