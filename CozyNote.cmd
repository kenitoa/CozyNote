@echo off
setlocal EnableExtensions

set "APP_DIR=%~dp0app"

if not exist "%APP_DIR%\pom.xml" (
    echo [CozyNote] App folder was not found: "%APP_DIR%"
    echo Keep this cmd file next to the app folder.
    pause
    exit /b 1
)

call :ensure_jdk
if errorlevel 1 exit /b 1

cd /d "%APP_DIR%"

where mvn >nul 2>nul
if errorlevel 1 (
    call :install_winget "Apache Maven" "Apache.Maven"
)

where mvn >nul 2>nul
if not errorlevel 1 (
    echo [CozyNote] Starting with Maven. Dependencies will be prepared automatically.
    mvn -q javafx:run
    set "APP_EXIT=%ERRORLEVEL%"
) else (
    echo [CozyNote] Maven was not found. Falling back to the local PowerShell runner.
    powershell -NoProfile -ExecutionPolicy Bypass -File "%APP_DIR%\run.ps1"
    set "APP_EXIT=%ERRORLEVEL%"
)

if not "%APP_EXIT%"=="0" (
    echo.
    echo [CozyNote] The app did not start successfully. Exit code: %APP_EXIT%
    pause
)

exit /b %APP_EXIT%

:ensure_jdk
where java >nul 2>nul
if errorlevel 1 goto install_jdk
where javac >nul 2>nul
if errorlevel 1 goto install_jdk
exit /b 0

:install_jdk
call :install_winget "Temurin JDK 21" "EclipseAdoptium.Temurin.21.JDK"
where java >nul 2>nul
if errorlevel 1 goto jdk_failed
where javac >nul 2>nul
if errorlevel 1 goto jdk_failed
exit /b 0

:jdk_failed
echo [CozyNote] Java JDK was not found after installation.
echo Close this window, open a new Command Prompt, and run CozyNote.cmd again.
pause
exit /b 1

:install_winget
set "PACKAGE_NAME=%~1"
set "PACKAGE_ID=%~2"
where winget >nul 2>nul
if errorlevel 1 (
    echo [CozyNote] %PACKAGE_NAME% is missing and winget is not available.
    exit /b 1
)
echo [CozyNote] Installing %PACKAGE_NAME%...
winget install --id %PACKAGE_ID% -e --source winget --accept-package-agreements --accept-source-agreements
exit /b %ERRORLEVEL%
