@echo off
rem Creates or updates the database. Usage: setup-database.bat [server] [database]
setlocal
set "SERVER=%~1"
if "%SERVER%"=="" set "SERVER=.\SQLEXPRESS"
set "DB=%~2"
if "%DB%"=="" set "DB=CLIENT_FILE_MANAGEMENT_SYSTEM"
cd /d "%~dp0"

sqlcmd -S "%SERVER%" -E -b -v DB="%DB%" -i sql\01_schema.sql || goto :failed
sqlcmd -S "%SERVER%" -E -b -v DB="%DB%" -i sql\02_procedures.sql || goto :failed
echo Database "%DB%" is ready on %SERVER%.
exit /b 0

:failed
echo Database setup failed. Check that SQL Server is running and that your Windows account can create databases.
exit /b 1
