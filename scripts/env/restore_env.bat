@echo off
rem Windows entry point for restore_env.ps1 (same folder); passes all arguments through.
rem Example: scripts\env\restore_env.bat -ClientTest
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0restore_env.ps1" %*
exit /b %ERRORLEVEL%
