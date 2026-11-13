@echo off

set TARGET_HOST=%1
set PORT=%2

set SNI_OPT=
if not "%3"=="" set SNI_OPT=-servername %3
rem OpenSSL 1.1.1+ derives SNI from the -connect hostname unless -noservername is given; older builds do not support it.
if "%3"=="" openssl s_client -help 2>&1 | findstr /C:"-noservername" >nul && set SNI_OPT=-noservername

echo | openssl s_client -connect %TARGET_HOST%:%PORT% %SNI_OPT% 2> null | openssl x509 -noout -enddate