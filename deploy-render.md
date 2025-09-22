# Deploy to Render - Complete Guide

## ✅ Changes Made for Render Compatibility

I've optimized your application for Render deployment with these changes:

### 1. **Port Configuration**
- Updated `application.properties` and `application-production.properties` to use `${PORT:8080}`
- Render will automatically assign a port via the `PORT` environment variable

### 2. **Health Check Endpoint**
- Changed health check from `/api/health/liveness` to `/actuator/health`
- This uses Spring Boot Actuator's built-in health endpoint

### 3. **Memory Optimization**
- Reduced JVM memory settings for Render's free tier (256MB-512MB)
- Optimized for Render's 512MB RAM limit

### 4. **Startup Script**
- Created `start-render.sh` for better process management
- Includes proper JVM tuning for Render environment

## 🚀 Deployment Steps

### Step 1: Push to GitHub
```bash
git add .
git commit -m "Optimize for Render deployment"
git push origin main
```

### Step 2: Create Render Account
1. Go to [render.com](https://render.com)
2. Sign up with your GitHub account
3. Authorize Render to access your repositories

### Step 3: Deploy Your Application
1. Click **"New +"** → **"Web Service"**
2. Connect your GitHub repository
3. Select your ATS repository
4. Configure the service:
   - **Name**: `ats-application` (or your preferred name)
   - **Environment**: `Java`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `./start-render.sh`
   - **Plan**: `Free`

### Step 4: Set Environment Variables
In the Render dashboard, add these environment variables:
- `SPRING_PROFILES_ACTIVE` = `production`
- `OPENAI_API_KEY` = `your-actual-openai-api-key`
- `PORT` = `10000` (Render will override this)

### Step 5: Deploy
1. Click **"Create Web Service"**
2. Wait for build and deployment (5-10 minutes)
3. Your app will be live at: `https://ats-application.onrender.com`

## 🔍 Health Check & Monitoring

### Health Endpoints
- **Main Health Check**: `https://your-app.onrender.com/actuator/health`
- **Detailed Health**: `https://your-app.onrender.com/actuator/health/liveness`

### Monitoring
- View real-time logs in Render dashboard
- Monitor memory usage and performance
- Set up alerts for downtime

## 🛠️ Troubleshooting

### Common Issues & Solutions

1. **Build Fails**
   - Check build logs in Render dashboard
   - Ensure all dependencies are in `pom.xml`
   - Verify Java version compatibility

2. **App Won't Start**
   - Check startup logs
   - Verify environment variables are set
   - Ensure OpenAI API key is valid

3. **Health Check Fails**
   - Verify the app is responding on the assigned port
   - Check if all required services are running

4. **Memory Issues**
   - Monitor memory usage in Render dashboard
   - Consider upgrading to paid plan if needed

## 📊 Render Free Tier Limits

- **750 hours/month** (31 days = 744 hours, so you get 6 hours buffer)
- **512MB RAM**
- **1GB disk space**
- **Apps sleep after 15 minutes of inactivity**
- **Cold start takes 30-60 seconds**

## 🎯 Your App URLs

After successful deployment:
- **Main App**: `https://ats-application.onrender.com`
- **Health Check**: `https://ats-application.onrender.com/actuator/health`
- **API Endpoints**: `https://ats-application.onrender.com/api/...`

## 🔄 Auto-Deploy

Render automatically redeploys when you push to your main branch. No manual intervention needed!
