@echo off
echo ========================================
echo ATS Application Setup
echo ========================================
echo.
echo This script will help you set up your OpenAI API key.
echo.
echo Please enter your OpenAI API key:
set /p OPENAI_API_KEY="API Key: "

if "%OPENAI_API_KEY%"=="" (
    echo Error: API key cannot be empty!
    pause
    exit /b 1
)

echo.
echo Setting environment variable...
setx OPENAI_API_KEY "%OPENAI_API_KEY%"

echo.
echo Environment variable set successfully!
echo.
echo Please restart your application for the changes to take effect.
echo.
echo To restart the backend:
echo 1. Stop the current Spring Boot application (Ctrl+C)
echo 2. Run: mvnw spring-boot:run
echo.
echo To restart the frontend:
echo 1. Stop the current React app (Ctrl+C)
echo 2. Run: npm start
echo.
pause 