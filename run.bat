@echo off
rem Starts Client Files, building it first if needed. Requires a JDK 17 or later.
setlocal
cd /d "%~dp0"

if not exist target\client-files.jar (
    echo Building the application, this only takes a while the first time...
    call mvnw.cmd -q -ntp package -DskipTests || exit /b 1
)
java -Djava.library.path=target\lib -jar target\client-files.jar
