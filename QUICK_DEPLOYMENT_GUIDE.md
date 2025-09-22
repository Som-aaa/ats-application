# Quick Deployment Guide

## 🚀 Step-by-Step Deployment

### 1. Deploy Backend to Render

1. **Go to [render.com](https://render.com)** and sign up/login
2. **Create New Web Service**:
   - Click "New +" → "Web Service"
   - Connect your GitHub account
   - Select your ATS repository
3. **Configure**:
   - **Name**: `ats-application`
   - **Environment**: `Docker`
   - **Plan**: `Free`
   - **Dockerfile Path**: `./Dockerfile`
   - **Health Check Path**: `/actuator/health`
4. **Environment Variables**:
   - Add `OPENAI_API_KEY` with your actual OpenAI API key
5. **Deploy**: Click "Create Web Service"
6. **Wait for deployment** (5-10 minutes)
7. **Copy your Render URL** (e.g., `https://ats-application-xxxx.onrender.com`)

### 2. Update Frontend Configuration

After getting your Render URL, run this PowerShell script:

```powershell
# Replace YOUR_RENDER_URL with your actual Render URL
.\update-backend-url.ps1 -BackendUrl "https://ats-application-xxxx.onrender.com"
```

**Or manually update**:
- Edit `ats-ui\.env.production` with your Render URL
- Edit `ats-ui\netlify.toml` with your Render URL

### 3. Deploy Frontend to Netlify

#### Option A: Netlify Dashboard (Recommended)
1. **Go to [netlify.com](https://netlify.com)** and sign up/login
2. **New site from Git**:
   - Click "New site from Git"
   - Connect your GitHub account
   - Select your ATS repository
3. **Build settings**:
   - **Base directory**: `ats-ui`
   - **Build command**: `npm run build`
   - **Publish directory**: `ats-ui/build`
4. **Environment variables**:
   - Add `REACT_APP_API_BASE_URL` with your Render URL
5. **Deploy**: Click "Deploy site"

#### Option B: Netlify CLI
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Navigate to frontend directory
cd ats-ui

# Login to Netlify
netlify login

# Deploy
netlify deploy --prod --dir=build
```

### 4. Test Your Deployment

1. **Backend**: Visit your Render URL + `/actuator/health`
2. **Frontend**: Visit your Netlify URL
3. **Test functionality**: Upload a resume and test the ATS features

## 🔧 Troubleshooting

### Backend Issues
- Check Render logs for errors
- Verify OpenAI API key is set correctly
- Ensure health check endpoint is working

### Frontend Issues
- Check Netlify build logs
- Verify environment variables are set
- Test API calls in browser console

### CORS Issues
- Your backend should already have CORS configured
- If you get CORS errors, check the backend CORS settings

## 📋 Checklist

- [ ] Backend deployed to Render
- [ ] Backend URL copied
- [ ] Frontend environment updated
- [ ] Frontend deployed to Netlify
- [ ] Both services tested and working
- [ ] CORS configured (if needed)

## 🎉 You're Done!

Your ATS application is now live with:
- **Backend**: Running on Render
- **Frontend**: Running on Netlify
- **Local Development**: Still works with localhost backend
