# Deploy to Fly.io - Step by Step Guide

## Prerequisites
1. Fly.io account (free)
2. Fly CLI installed
3. OpenAI API key

## Install Fly CLI
```bash
# Windows (PowerShell)
iwr https://fly.io/install.ps1 -useb | iex

# Or download from: https://fly.io/docs/hands-on/install-flyctl/
```

## Steps to Deploy

### 1. Login to Fly.io
```bash
fly auth login
```

### 2. Initialize Fly App
```bash
fly launch
# Follow prompts:
# - App name: ats-application (or choose your own)
# - Region: Choose closest to your users
# - Deploy now: No (we'll set env vars first)
```

### 3. Set Environment Variables
```bash
fly secrets set OPENAI_API_KEY=your-openai-api-key-here
fly secrets set SPRING_PROFILES_ACTIVE=production
```

### 4. Deploy
```bash
fly deploy
```

### 5. Open Your App
```bash
fly open
```

## Commands
- `fly status` - Check app status
- `fly logs` - View logs
- `fly scale memory 512` - Scale memory
- `fly secrets list` - List environment variables

## Your App URL
After deployment: `https://ats-application.fly.dev`

## Benefits of Fly.io
- Global edge deployment
- Fast cold starts
- Automatic HTTPS
- Built-in load balancing
- Easy scaling
