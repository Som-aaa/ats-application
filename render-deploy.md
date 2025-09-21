# Render Free Deployment Guide

## Step 1: Prepare Your Code
1. Push your code to GitHub
2. Ensure you have `Dockerfile` (already created)

## Step 2: Deploy to Render
1. Go to [render.com](https://render.com)
2. Sign up with GitHub
3. Click "New" → "Web Service"
4. Connect your GitHub repository
5. Select your ATS repository

## Step 3: Configure Settings
- **Build Command**: `mvn clean package -DskipTests`
- **Start Command**: `java -jar target/ATS-0.0.1-SNAPSHOT.jar`
- **Environment**: Java

## Step 4: Add Environment Variables
In Render dashboard, add:
```
OPENAI_API_KEY=your-openai-api-key-here
SPRING_PROFILES_ACTIVE=production
JAVA_OPTS=-Xms256m -Xmx512m
```

## Step 5: Deploy
- Render will build and deploy automatically
- You'll get a free URL like: `https://your-app-name.onrender.com`

## Free Tier Limits:
- 750 hours/month
- 512MB RAM
- Automatic sleep after 15 minutes of inactivity
- Custom domain support
