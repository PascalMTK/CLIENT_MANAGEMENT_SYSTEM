@echo off
rem Adds sample accounts and documents for a demonstration.
rem Does nothing if the database already has an administrator.
setlocal
cd /d "%~dp0"

if not exist target\client-files.jar (
    call mvnw.cmd -q -ntp package -DskipTests || exit /b 1
)
java -Djava.library.path=target\lib -cp "target\client-files.jar;target\lib\*" com.clientfiles.dev.DemoData
