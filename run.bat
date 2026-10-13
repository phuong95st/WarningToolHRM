@echo off
chcp 65001 > nul
cd /d "%~dp0"
set PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
title HRM Attendance Warning Tool
echo ===================================================
echo     Starting HRM Attendance Warning Tool...
echo ===================================================

if not exist "target\WarningToolHRM-1.0.0.jar" (
    echo [INFO] JAR file not found. Building project with Maven...
    call mvn clean package -DskipTests
)

java -Djava.io.tmpdir="%~dp0temp" -jar target\WarningToolHRM-1.0.0.jar
pause
