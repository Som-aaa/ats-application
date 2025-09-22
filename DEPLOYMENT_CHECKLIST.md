# 🚀 Complete Deployment Checklist

## ✅ Pre-Deployment Setup

### 1. Code Preparation
- [x] Environment variables configured
- [x] CORS settings updated for production
- [x] Port configuration fixed (10000 for Render)
- [x] Production application properties created
- [x] Dockerfile optimized for production
- [x] render.yaml configured

### 2. Frontend Configuration
- [x] API URLs use environment variables
- [x] Development environment (.env.development) created
- [x] Production environment (.env.production) created
- [x] Netlify configuration (netlify.toml) created

## 🎯 Step-by-Step Deployment

### Step 1: Deploy Backend to Render

1. **Push to GitHub**:
   ```bash
   git add .
   git commit -m "Prepare for deployment"
   git push origin main
   ```

2. **Go to Render**:
   - Visit [render.com](https://render.com)
   - Sign up/Login with GitHub

3. **Create Web Service**:
   - Click "New +" → "Web Service"
   - Connect GitHub repository
   - Select your ATS repository

4. **Configure Service**:
   - **Name**: `ats-application`
   - **Environment**: `Docker`
   - **Plan**: `Free`
   - **Dockerfile Path**: `./Dockerfile`
   - **Health Check Path**: `/actuator/health`

5. **Environment Variables**:
   - Add `OPENAI_API_KEY` with your actual API key
   - Other variables are already configured in render.yaml

6. **Deploy**:
   - Click "Create Web Service"
   - Wait 5-10 minutes for deployment

7. **Get Your Backend URL**:
   - Copy the URL (e.g., `https://ats-application-xxxx.onrender.com`)

### Step 2: Update Frontend Configuration

1. **Update Environment Files**:
   ```powershell
   # Replace with your actual Render URL
   .\update-backend-url.ps1 -BackendUrl "https://ats-application-xxxx.onrender.com"
   ```

2. **Or Manually Update**:
   - Edit `ats-ui\.env.production`
   - Edit `ats-ui\netlify.toml`

### Step 3: Deploy Frontend to Netlify

1. **Go to Netlify**:
   - Visit [netlify.com](https://netlify.com)
   - Sign up/Login with GitHub

2. **Create New Site**:
   - Click "New site from Git"
   - Connect GitHub repository
   - Select your ATS repository

3. **Build Settings**:
   - **Base directory**: `ats-ui`
   - **Build command**: `npm run build`
   - **Publish directory**: `ats-ui/build`

4. **Environment Variables**:
   - Add `REACT_APP_API_BASE_URL` with your Render URL

5. **Deploy**:
   - Click "Deploy site"
   - Wait for build to complete

## 🧪 Testing Your Deployment

### Backend Tests
1. **Health Check**: Visit `https://your-render-url.onrender.com/actuator/health`
2. **API Test**: Test a simple API endpoint
3. **Logs**: Check Render logs for any errors

### Frontend Tests
1. **Visit Netlify URL**: Open your deployed frontend
2. **Test Upload**: Try uploading a resume
3. **Check Console**: Look for any CORS or API errors
4. **Test All Features**: Go through all ATS functionality

## 🔧 Troubleshooting

### Common Issues

#### Backend Issues
- **Build Fails**: Check Dockerfile and dependencies
- **Health Check Fails**: Verify port configuration (10000)
- **API Key Error**: Ensure OPENAI_API_KEY is set correctly
- **Memory Issues**: Check JAVA_OPTS in render.yaml

#### Frontend Issues
- **Build Fails**: Check for JavaScript/TypeScript errors
- **API Calls Fail**: Verify REACT_APP_API_BASE_URL is correct
- **CORS Errors**: Check backend CORS configuration
- **Environment Variables**: Ensure they're set in Netlify

#### CORS Issues
- Backend allows all origins with `allowedOriginPatterns("*")`
- If still having issues, check browser console for specific errors

### Debug Commands

```bash
# Test backend locally
./mvnw spring-boot:run

# Test frontend locally
cd ats-ui
npm start

# Check environment variables
echo $REACT_APP_API_BASE_URL
```

## 📊 Monitoring

### Render Dashboard
- Monitor CPU and memory usage
- Check logs for errors
- Monitor uptime and response times

### Netlify Dashboard
- Monitor build status
- Check deployment logs
- Monitor site performance

## 🎉 Success Indicators

- [ ] Backend health check returns 200 OK
- [ ] Frontend loads without errors
- [ ] Resume upload works
- [ ] ATS analysis completes successfully
- [ ] No CORS errors in browser console
- [ ] All API endpoints respond correctly

## 📝 Important URLs

After deployment, you'll have:
- **Backend**: `https://ats-application-xxxx.onrender.com`
- **Frontend**: `https://your-site-name.netlify.app`
- **Health Check**: `https://ats-application-xxxx.onrender.com/actuator/health`

## 🔄 Local Development

Your local development setup remains unchanged:
- Backend: `http://localhost:8080` (or 10000)
- Frontend: `http://localhost:3000`
- Uses `.env.development` for local API calls

## 📞 Support

If you encounter issues:
1. Check the logs in both Render and Netlify dashboards
2. Verify all environment variables are set correctly
3. Test API endpoints directly using curl or Postman
4. Check browser console for frontend errors
5. Ensure your OpenAI API key is valid and has credits
