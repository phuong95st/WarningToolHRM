Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")
strCurrentDir = FSO.GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = strCurrentDir

' Run javaw with 0 (hidden window)
WshShell.Run "javaw -jar """ & strCurrentDir & "\target\WarningToolHRM-1.0.0.jar""", 0, False
