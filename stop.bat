@echo off
chcp 65001 > nul
cd /d "%~dp0"
echo ===================================================
echo     Stopping HRM Attendance Warning Tool...
echo ===================================================

wmic process where "CommandLine like '%%WarningToolHRM%%'" call terminate > nul 2>&1
echo Đã dừng tiến trình HRM Attendance Warning Tool (nếu đang chạy).
pause
