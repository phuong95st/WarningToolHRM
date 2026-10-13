Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")
strCurrentDir = FSO.GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = strCurrentDir

' Skip downloading heavy browsers since we use pre-installed Edge/Chrome
WshShell.Environment("PROCESS")("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD") = "1"

' Run javaw with 0 (hidden window), redirecting temp to D: drive to protect C: drive space
WshShell.Run "javaw -Djava.io.tmpdir=""" & strCurrentDir & "\temp"" -jar """ & strCurrentDir & "\target\WarningToolHRM-1.0.0.jar""", 0, False
