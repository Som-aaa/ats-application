# PowerShell script to update backend URL for deployment
# Run this script after you get your Render URL

param(
    [Parameter(Mandatory=$true)]
    [string]$BackendUrl
)

Write-Host "Updating backend URL to: $BackendUrl" -ForegroundColor Green

# Update .env.production
$envContent = "REACT_APP_API_BASE_URL=$BackendUrl"
Set-Content -Path "ats-ui\.env.production" -Value $envContent
Write-Host "Updated .env.production" -ForegroundColor Green

# Update netlify.toml
$netlifyContent = @"
[build]
  base = "."
  publish = "build"
  command = "npm run build"

[build.environment]
  REACT_APP_API_BASE_URL = "$BackendUrl"

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200

[[headers]]
  for = "/*"
  [headers.values]
    X-Frame-Options = "DENY"
    X-XSS-Protection = "1; mode=block"
    X-Content-Type-Options = "nosniff"
    Referrer-Policy = "strict-origin-when-cross-origin"

[[headers]]
  for = "/api/*"
  [headers.values]
    Access-Control-Allow-Origin = "*"
    Access-Control-Allow-Methods = "GET, POST, PUT, DELETE, OPTIONS"
    Access-Control-Allow-Headers = "Content-Type, Authorization"
"@

Set-Content -Path "ats-ui\netlify.toml" -Value $netlifyContent
Write-Host "Updated netlify.toml" -ForegroundColor Green

Write-Host "Backend URL updated successfully!" -ForegroundColor Green
Write-Host "You can now deploy to Netlify." -ForegroundColor Yellow
