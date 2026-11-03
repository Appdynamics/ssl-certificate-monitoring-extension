@echo off

set TARGET_HOST=%1
set PORT=%2

set SNI_OPT=
if not "%3"=="" set SNI_OPT=-servername %3

echo | openssl s_client -connect %TARGET_HOST%:%PORT% %SNI_OPT% 2> null | openssl x509 -noout -enddate