# PowerShell script to prepare and deploy to Render
# This script prepares your code for Render deployment

Write-Host "🚀 Preparing ATS Application for Render Deployment" -ForegroundColor Green

# Check if we're in the right directory
if (-not (Test-Path "pom.xml")) {
    Write-Host "❌ Error: pom.xml not found. Please run this script from the ATS root directory." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Found pom.xml - we're in the right directory" -ForegroundColor Green

# Check if Dockerfile exists
if (-not (Test-Path "Dockerfile")) {
    Write-Host "❌ Error: Dockerfile not found." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Found Dockerfile" -ForegroundColor Green

# Check if render.yaml exists
if (-not (Test-Path "render.yaml")) {
    Write-Host "❌ Error: render.yaml not found." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Found render.yaml" -ForegroundColor Green

# Test Maven build
Write-Host "🔨 Testing Maven build..." -ForegroundColor Yellow
try {
    & .\mvnw.cmd clean compile -q
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Maven build successful" -ForegroundColor Green
    } else {
        Write-Host "❌ Maven build failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Error running Maven build: $_" -ForegroundColor Red
    exit 1
}

# Check if OpenAI API key is set
if (-not $env:OPENAI_API_KEY) {
    Write-Host "⚠️  Warning: OPENAI_API_KEY environment variable not set" -ForegroundColor Yellow
    Write-Host "   You'll need to set this in Render dashboard" -ForegroundColor Yellow
} else {
    Write-Host "✅ OPENAI_API_KEY is set" -ForegroundColor Green
}

Write-Host ""
Write-Host "🎯 Next Steps:" -ForegroundColor Cyan
Write-Host "1. Push your code to GitHub" -ForegroundColor White
Write-Host "2. Go to https://render.com" -ForegroundColor White
Write-Host "3. Create new Web Service" -ForegroundColor White
Write-Host "4. Connect your GitHub repository" -ForegroundColor White
Write-Host "5. Use these settings:" -ForegroundColor White
Write-Host "   - Name: ats-application" -ForegroundColor White
Write-Host "   - Environment: Docker" -ForegroundColor White
Write-Host "   - Plan: Free" -ForegroundColor White
Write-Host "   - Dockerfile Path: ./Dockerfile" -ForegroundColor White
Write-Host "   - Health Check Path: /actuator/health" -ForegroundColor White
Write-Host "6. Add environment variable: OPENAI_API_KEY" -ForegroundColor White
Write-Host "7. Deploy!" -ForegroundColor White
Write-Host ""
Write-Host "📝 Your Render URL will be: https://ats-application-xxxx.onrender.com" -ForegroundColor Yellow
Write-Host "   (Replace xxxx with your actual service ID)" -ForegroundColor Yellow
Write-Host ""
Write-Host "✅ Code is ready for deployment!" -ForegroundColor Green
