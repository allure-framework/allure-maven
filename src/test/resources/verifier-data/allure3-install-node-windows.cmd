@echo off
setlocal EnableExtensions DisableDelayedExpansion

if not exist "@capture.parent@" mkdir "@capture.parent@"
type nul > "@capture.file@"
>> "@capture.file@" echo(cli=%~1

set "PREFIX="
:loop
shift
if "%~1"=="" goto done
>> "@capture.file@" echo(arg=%~1
if not "%~1"=="--prefix" goto loop
shift
set "PREFIX=%~1"
>> "@capture.file@" echo(arg=%~1
goto loop

:done
if not defined PREFIX exit /b 1
if not exist "%PREFIX%\node_modules\allure" mkdir "%PREFIX%\node_modules\allure"
echo console.log("fake allure") > "%PREFIX%\node_modules\allure\cli.js"
exit /b 0
