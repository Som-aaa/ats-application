# Google Cloud Run Free Deployment Guide

## Step 1: Setup Google Cloud
1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create a new project (or use existing)
3. Enable Cloud Run API
4. Install Google Cloud CLI

## Step 2: Deploy to Cloud Run
```bash
# Login to Google Cloud
gcloud auth login

# Set your project
gcloud config set project YOUR_PROJECT_ID

# Build and deploy
gcloud run deploy ats-app \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars OPENAI_API_KEY=your-openai-api-key-here,SPRING_PROFILES_ACTIVE=production
```

## Step 3: Access Your App
- Google will provide a URL like: `https://ats-app-xxx-uc.a.run.app`

## Free Tier Limits:
- 2 million requests/month
- 400,000 GB-seconds of memory
- 200,000 vCPU-seconds
- Custom domain support
