@echo off
setlocal EnableExtensions DisableDelayedExpansion

if not exist "@capture.parent@" mkdir "@capture.parent@"
>> "@capture.file@" echo(cli=%~1
set "COMMAND=%~2"
>> "@capture.file@" echo(command=%COMMAND%

shift
:loop
if "%~1"=="" goto afterCapture
>> "@capture.file@" echo(arg=%~1
shift
goto loop

:afterCapture
>> "@capture.file@" echo(---

if /I not "%COMMAND%"=="generate" exit /b 0

if not exist "@cases.directory@" mkdir "@cases.directory@"
type nul > "@index.file@"
type nul > "@data.directory@\behaviors.json"
type nul > "@data.directory@\categories.json"
type nul > "@data.directory@\packages.json"
type nul > "@data.directory@\timeline.json"
type nul > "@data.directory@\suites.json"
echo {} > "@cases.directory@\case.json"
exit /b 0
