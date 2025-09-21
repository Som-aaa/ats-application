Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ATS API Key Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if api-key.txt exists
if (-not (Test-Path "api-key.txt")) {
    Write-Host "Error: api-key.txt file not found!" -ForegroundColor Red
    Write-Host "Please create api-key.txt and paste your OpenAI API key in it." -ForegroundColor Yellow
    Read-Host "Press Enter to continue"
    exit 1
}

# Read the API key from the file
$apiKeyContent = Get-Content "api-key.txt" -Raw
$lines = $apiKeyContent -split "`n"

# Find the API key (skip comments and empty lines)
$apiKey = ""
foreach ($line in $lines) {
    $trimmedLine = $line.Trim()
    if ($trimmedLine -and -not $trimmedLine.StartsWith("#") -and $trimmedLine -ne "your-openai-api-key-here") {
        $apiKey = $trimmedLine
        break
    }
}

if (-not $apiKey -or $apiKey -eq "your-openai-api-key-here") {
    Write-Host "Error: No valid API key found in api-key.txt!" -ForegroundColor Red
    Write-Host "Please edit api-key.txt and paste your actual OpenAI API key." -ForegroundColor Yellow
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host "Found API key: $($apiKey.Substring(0, 7))..." -ForegroundColor Green
Write-Host ""

# Set environment variable
[Environment]::SetEnvironmentVariable("OPENAI_API_KEY", $apiKey, "User")

Write-Host "API key set successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Restart your terminal/command prompt" -ForegroundColor White
Write-Host "2. Restart the backend application" -ForegroundColor White
Write-Host "3. Test the application" -ForegroundColor White
Write-Host ""

Read-Host "Press Enter to continue" 