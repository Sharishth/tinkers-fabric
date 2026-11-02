@echo off
rem Windows entry point for clean_env.ps1 (same folder); passes all arguments through.
rem Example: scripts\env\clean_env.bat -DryRun
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0clean_env.ps1" %*
exit /b %ERRORLEVEL%
