# Railway Free Deployment Guide

## Step 1: Prepare Your Code
1. Make sure your code is in a Git repository (GitHub, GitLab, etc.)
2. Ensure you have a `Dockerfile` (already created)

## Step 2: Deploy to Railway
1. Go to [railway.app](https://railway.app)
2. Sign up with GitHub
3. Click "New Project" → "Deploy from GitHub repo"
4. Select your ATS repository
5. Railway will automatically detect the Dockerfile

## Step 3: Configure Environment Variables
In Railway dashboard, go to Variables tab and add:
```
OPENAI_API_KEY=your-openai-api-key-here
SPRING_PROFILES_ACTIVE=production
JAVA_OPTS=-Xms256m -Xmx512m
```

## Step 4: Deploy
- Railway will automatically build and deploy
- You'll get a free URL like: `https://your-app-name.railway.app`

## Free Tier Limits:
- 500 hours/month (enough for personal use)
- 1GB RAM
- 1GB storage
- Custom domain support
