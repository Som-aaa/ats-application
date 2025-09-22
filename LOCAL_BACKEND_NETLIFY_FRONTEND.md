# Local Backend + Netlify Frontend Deployment Guide

This guide explains how to run your Spring Boot backend locally while deploying your React frontend to Netlify.

## Overview

- **Backend**: Runs locally on `http://localhost:8080`
- **Frontend**: Deployed to Netlify with environment-based API configuration
- **Development**: Frontend uses local backend
- **Production**: Frontend uses deployed backend

## Prerequisites

1. **Backend deployed somewhere** (Render, Railway, Heroku, etc.)
2. **Netlify account** (free tier available)
3. **Git repository** (GitHub, GitLab, or Bitbucket)

## Step 1: Deploy Your Backend

First, deploy your Spring Boot backend to a cloud service. Here are popular options:

### Option A: Render (Recommended)
1. Push your code to GitHub
2. Connect your repository to Render
3. Deploy as a Web Service
4. Note your backend URL (e.g., `https://your-app-name.onrender.com`)

### Option B: Railway
1. Connect your GitHub repository to Railway
2. Deploy your Spring Boot app
3. Note your backend URL (e.g., `https://your-app-name.railway.app`)

### Option C: Heroku
1. Create a Heroku app
2. Deploy using the Heroku CLI or GitHub integration
3. Note your backend URL (e.g., `https://your-app-name.herokuapp.com`)

## Step 2: Update Environment Configuration

### For Development (Local Backend)
The `.env.development` file is already configured:
```
REACT_APP_API_BASE_URL=http://localhost:8080
```

### For Production (Deployed Backend)
Update `.env.production` with your actual backend URL:
```
REACT_APP_API_BASE_URL=https://your-actual-backend-url.com
```

Also update `netlify.toml`:
```toml
[build.environment]
  REACT_APP_API_BASE_URL = "https://your-actual-backend-url.com"
```

## Step 3: Deploy to Netlify

### Method 1: Netlify Dashboard (Recommended)
1. Go to [netlify.com](https://netlify.com) and sign in
2. Click "New site from Git"
3. Connect your GitHub/GitLab/Bitbucket repository
4. Configure build settings:
   - **Base directory**: `ats-ui`
   - **Build command**: `npm run build`
   - **Publish directory**: `ats-ui/build`
5. Add environment variable:
   - Key: `REACT_APP_API_BASE_URL`
   - Value: `https://your-backend-url.com`
6. Click "Deploy site"

### Method 2: Netlify CLI
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Navigate to your frontend directory
cd ats-ui

# Build the project
npm run build

# Deploy to Netlify
netlify deploy --prod --dir=build
```

## Step 4: Configure CORS (Important!)

Your backend needs to allow requests from your Netlify domain. Update your Spring Boot application:

### Add CORS Configuration
Create or update your CORS configuration in your Spring Boot app:

```java
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:3000",  // Local development
                    "https://your-netlify-site.netlify.app",  // Your Netlify domain
                    "https://your-custom-domain.com"  // If you have a custom domain
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

## Step 5: Test Your Setup

### Local Development
1. Start your backend locally:
   ```bash
   cd ATS
   ./mvnw spring-boot:run
   ```

2. Start your frontend locally:
   ```bash
   cd ats-ui
   npm start
   ```

3. Your frontend will use `http://localhost:8080` for API calls

### Production Testing
1. Deploy your frontend to Netlify
2. Visit your Netlify URL
3. Your frontend will use your deployed backend URL for API calls

## Environment Variables Summary

| Environment | REACT_APP_API_BASE_URL | Backend Location |
|-------------|------------------------|------------------|
| Development | `http://localhost:8080` | Local machine |
| Production | `https://your-backend-url.com` | Cloud service |

## Troubleshooting

### CORS Errors
- Ensure your backend CORS configuration includes your Netlify domain
- Check that your backend is actually deployed and accessible

### API Connection Issues
- Verify your backend URL is correct
- Check that your backend is running and healthy
- Test your backend API endpoints directly

### Build Issues
- Ensure all dependencies are installed: `npm install`
- Check for any TypeScript/JavaScript errors
- Verify environment variables are set correctly

## Benefits of This Setup

1. **Cost-effective**: Free hosting for frontend, minimal cost for backend
2. **Fast development**: Local backend for quick iteration
3. **Scalable**: Easy to scale frontend with Netlify's CDN
4. **Flexible**: Can easily switch between local and production backends
5. **Professional**: Separate concerns between frontend and backend

## Next Steps

1. Deploy your backend to a cloud service
2. Update the environment variables with your actual backend URL
3. Deploy your frontend to Netlify
4. Test both local development and production setups
5. Consider setting up a custom domain for your Netlify site

## Support

If you encounter issues:
1. Check the browser console for errors
2. Verify your backend is running and accessible
3. Check Netlify's build logs
4. Ensure CORS is properly configured
