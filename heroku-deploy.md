# Heroku Free Deployment Guide

## Step 1: Install Heroku CLI
Download from [devcenter.heroku.com](https://devcenter.heroku.com/articles/heroku-cli)

## Step 2: Prepare Your Code
1. Push to GitHub
2. Ensure you have `Procfile` (already created)

## Step 3: Deploy to Heroku
```bash
# Login to Heroku
heroku login

# Create Heroku app
heroku create your-ats-app-name

# Set environment variables
heroku config:set OPENAI_API_KEY=your-openai-api-key-here
heroku config:set SPRING_PROFILES_ACTIVE=production
heroku config:set JAVA_OPTS=-Xmx512m -Xms256m

# Deploy
git push heroku main
```

## Step 4: Open Your App
```bash
heroku open
```

## Free Tier Limits:
- 550-1000 dyno hours/month
- 512MB RAM
- App sleeps after 30 minutes of inactivity
- Custom domain support
