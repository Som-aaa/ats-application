Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ATS Application Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "This script will help you set up your OpenAI API key." -ForegroundColor Yellow
Write-Host ""

$apiKey = Read-Host "Please enter your OpenAI API key" -AsSecureString

if ($apiKey.Length -eq 0) {
    Write-Host "Error: API key cannot be empty!" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host ""
Write-Host "Setting environment variable..." -ForegroundColor Green

# Convert secure string to plain text
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($apiKey)
$plainApiKey = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)

# Set environment variable
[Environment]::SetEnvironmentVariable("OPENAI_API_KEY", $plainApiKey, "User")

Write-Host ""
Write-Host "Environment variable set successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Please restart your application for the changes to take effect." -ForegroundColor Yellow
Write-Host ""
Write-Host "To restart the backend:" -ForegroundColor White
Write-Host "1. Stop the current Spring Boot application (Ctrl+C)" -ForegroundColor Gray
Write-Host "2. Run: ./mvnw spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "To restart the frontend:" -ForegroundColor White
Write-Host "1. Stop the current React app (Ctrl+C)" -ForegroundColor Gray
Write-Host "2. Run: npm start" -ForegroundColor Gray
Write-Host ""

Read-Host "Press Enter to continue" 