@echo off
rem Tinkers Fabric daily dev commands: build, test and run (Windows).
rem Usage: scripts\dev.bat <command>   (run "scripts\dev.bat help" for the list)
rem The JDK must match java_version in gradle.properties. Set TF_JAVA_HOME, or put
rem set "JAVA_HOME=<jdk path>" in scripts\dev.local.bat (git-ignored; scripts\env\restore_env.bat writes it).
rem Full machine clean/restore lives in scripts\env\.
setlocal EnableExtensions

rem Always work from the repo root, wherever the script is called from
pushd "%~dp0.." || exit /b 1
rem WHY: full path; some shells set NoDefaultCurrentDirectoryInExePath, so a bare gradlew.bat is not found
set "GRADLEW=%CD%\gradlew.bat"
rem Required Java version comes from gradle.properties (single source of truth)
set "REQUIRED_JAVA="
for /f "tokens=2 delims==" %%V in ('findstr /b /c:"java_version=" gradle.properties') do set "REQUIRED_JAVA=%%V"

if exist "scripts\dev.local.bat" call "scripts\dev.local.bat"
if defined TF_JAVA_HOME set "JAVA_HOME=%TF_JAVA_HOME%"

set "COMMAND=%~1"
if "%COMMAND%"=="" set "COMMAND=help"

if /i "%COMMAND%"=="help" goto :help
call :require_java || goto :fail
if /i "%COMMAND%"=="check" goto :check
if /i "%COMMAND%"=="clean" goto :clean
if /i "%COMMAND%"=="reset" goto :reset
if /i "%COMMAND%"=="build" goto :build
if /i "%COMMAND%"=="test" goto :test
if /i "%COMMAND%"=="client-test" goto :client_test
if /i "%COMMAND%"=="client" goto :client
if /i "%COMMAND%"=="server" goto :server
if /i "%COMMAND%"=="all" goto :all
echo Unknown command: %COMMAND%
goto :fail

:help
echo Tinkers Fabric dev script (needs Java %REQUIRED_JAVA%, from gradle.properties)
echo.
echo   check        Show Java, Gradle and git versions
echo   clean        Delete build outputs (gradlew clean)
echo   reset        clean + delete dev run folders (test worlds, logs, options)
echo   build        Compile, unit tests and server GameTests
echo   test         Unit tests and server GameTests only
echo   client-test  Client game test: boots to title, opens a world, saves screenshots
echo   client       Start the dev client (tconstruct + mantle)
echo   server       Start the dev server (needs TF_ACCEPT_EULA=1, see below)
echo   all          reset, build, client-test: full fresh verification
echo.
echo Java: set TF_JAVA_HOME or run scripts\env\restore_env.bat once (writes scripts\dev.local.bat).
echo Full clean incl. Loom caches: scripts\env\clean_env.bat -DryRun
echo Server: running a server means accepting the Minecraft EULA (https://aka.ms/MinecraftEULA).
echo         Set TF_ACCEPT_EULA=1 only if you accept it.
goto :done

:check
"%JAVA_HOME%\bin\java.exe" -version
call "%GRADLEW%" --version || goto :fail
git --version
goto :done

:clean
call "%GRADLEW%" clean || goto :fail
goto :done

:reset
call :reset_dirs || goto :fail
goto :done

:build
call "%GRADLEW%" build || goto :fail
goto :done

:test
call "%GRADLEW%" test :mantle:runGameTest :tconstruct:runGameTest || goto :fail
goto :done

:client_test
call "%GRADLEW%" :tconstruct:runClientGameTest || goto :fail
echo Screenshots: tconstruct\build\run\clientGameTest\screenshots
goto :done

:client
call "%GRADLEW%" :tconstruct:runClient || goto :fail
goto :done

:server
if not "%TF_ACCEPT_EULA%"=="1" (
  echo The dev server needs the Minecraft EULA accepted: https://aka.ms/MinecraftEULA
  echo If you accept it, run: set TF_ACCEPT_EULA=1 ^&^& scripts\dev.bat server
  goto :fail
)
if not exist "tconstruct\run" mkdir "tconstruct\run"
> "tconstruct\run\eula.txt" echo eula=true
call "%GRADLEW%" :tconstruct:runServer || goto :fail
goto :done

:all
call :reset_dirs || goto :fail
call "%GRADLEW%" build || goto :fail
call "%GRADLEW%" :tconstruct:runClientGameTest || goto :fail
echo Screenshots: tconstruct\build\run\clientGameTest\screenshots
goto :done

rem --- helpers ---

:reset_dirs
call "%GRADLEW%" clean || exit /b 1
for %%D in (mantle tconstruct) do (
  if exist "%%D\run" (
    echo Removing %%D\run
    rmdir /s /q "%%D\run"
  )
)
exit /b 0

:require_java
if not defined REQUIRED_JAVA (
  echo java_version is missing from gradle.properties.
  exit /b 1
)
if not defined JAVA_HOME (
  echo JAVA_HOME is not set. Java %REQUIRED_JAVA% is required.
  echo Set TF_JAVA_HOME or run scripts\env\restore_env.bat once.
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo No java.exe under JAVA_HOME=%JAVA_HOME%
  exit /b 1
)
rem WHY: Minecraft and Loom need this exact Java; an older JDK fails late with confusing errors
rem The "." before the version stands for the quote; an escaped quote breaks cmd parsing.
rem Via a temp file: piping into findstr makes java print "nonexistent pipe" errors.
set "JAVA_VERSION_FILE=%TEMP%\tinkers-fabric-java-version.txt"
"%JAVA_HOME%\bin\java.exe" -version > "%JAVA_VERSION_FILE%" 2>&1
findstr /r /c:"version .%REQUIRED_JAVA%[^0-9]" "%JAVA_VERSION_FILE%" >nul
set "JAVA_MATCH=%ERRORLEVEL%"
del "%JAVA_VERSION_FILE%" >nul 2>&1
if not "%JAVA_MATCH%"=="0" (
  echo JAVA_HOME=%JAVA_HOME% is not Java %REQUIRED_JAVA%:
  "%JAVA_HOME%\bin\java.exe" -version
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
exit /b 0

:done
popd
exit /b 0

:fail
popd
exit /b 1
