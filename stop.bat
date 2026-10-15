@echo off
chcp 65001 > nul
cd /d "%~dp0"
echo ===================================================
echo     Stopping HRM Attendance Warning Tool...
echo ===================================================

powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*WarningToolHRM*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Host ('Da tat tien trinh PID: ' + $_.ProcessId) }"
echo Da tat tat ca tien trinh WarningToolHRM dang chay.
pause
