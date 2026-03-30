@echo off
REM Basic Computer - Build and Run Script

echo ========================================
echo Building Basic Computer Application
echo ========================================

REM Check for PostgreSQL JDBC driver (check for both versions)
if exist "lib\postgresql-42.7.8.jar" (
    set JDBC_JAR=lib\postgresql-42.7.8.jar
) else if exist "lib\postgresql-42.7.6.jar" (
    set JDBC_JAR=lib\postgresql-42.7.6.jar
) else (
    echo Error: PostgreSQL JDBC driver not found!
    echo Please download postgresql-42.7.8.jar and place it in the lib folder
    echo Download from: https://jdbc.postgresql.org/download/postgresql-42.7.8.jar
    pause
    exit /b 1
)

REM Check for BCrypt library
if exist "lib\jbcrypt-0.4.jar" (
    set BCRYPT_JAR=lib\jbcrypt-0.4.jar
) else (
    echo Error: BCrypt library not found!
    echo Please download jbcrypt-0.4.jar and place it in the lib folder
    echo Download from: https://mvnrepository.com/artifact/org.mindrot/jbcrypt/0.4
    pause
    exit /b 1
)

echo Using JDBC driver: %JDBC_JAR%
echo Using BCrypt library: %BCRYPT_JAR%

REM Clean previous class files
if exist bin rmdir /s /q bin
mkdir bin

REM Compile the application
echo Compiling Java files...
javac -d bin -cp ".;%JDBC_JAR%;%BCRYPT_JAR%" -encoding UTF-8 src\com\basiccomputer\*.java src\com\basiccomputer\db\*.java src\com\basiccomputer\handler\*.java src\com\basiccomputer\util\*.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo ========================================
echo Starting Basic Computer Server
echo ========================================
echo Server running at: http://localhost:8082
echo.

REM Run the application
java -cp "bin;%JDBC_JAR%;%BCRYPT_JAR%" com.basiccomputer.Main

pause