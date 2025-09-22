# Deploy UI + Backend as Single Service on Render

## ✅ **What I've Done**

I've configured your application to deploy both the React UI and Spring Boot backend as a **single web service** on Render:

### **1. UI Integration**
- ✅ Updated `render.yaml` to build React UI during deployment
- ✅ Created `integrate-ui.sh` script to build and copy UI to Spring Boot static resources
- ✅ Added `UIController.java` to serve React app for all non-API routes
- ✅ Updated startup script to handle UI integration

### **2. CORS Configuration**
- ✅ Updated CORS settings to allow requests from the deployed domain
- ✅ Configured both localhost (development) and production URLs

### **3. Build Process**
- ✅ Modified build command to: `cd ats-ui && npm install && npm run build && cd .. && mvn clean package -DskipTests`
- ✅ UI gets built and integrated automatically during deployment

## 🚀 **Deployment Steps**

### **Step 1: Push to GitHub**
```bash
git add .
git commit -m "Integrate UI with backend for single service deployment"
git push origin main
```

### **Step 2: Deploy on Render**
1. Go to [render.com](https://render.com)
2. Click **"New +"** → **"Web Service"**
3. Connect your GitHub repository
4. Configure:
   - **Name**: `ats-application`
   - **Environment**: `Java`
   - **Build Command**: `cd ats-ui && npm install && npm run build && cd .. && mvn clean package -DskipTests`
   - **Start Command**: `./start-render.sh`
   - **Plan**: `Free`

### **Step 3: Set Environment Variables**
- `SPRING_PROFILES_ACTIVE` = `production`
- `OPENAI_API_KEY` = `your-openai-api-key`

### **Step 4: Deploy**
- Click **"Create Web Service"**
- Wait for build and deployment (10-15 minutes)
- Your complete app will be live at: `https://ats-application.onrender.com`

## 🌐 **How It Works**

### **Single URL for Everything**
- **Main App**: `https://ats-application.onrender.com` (React UI)
- **API Endpoints**: `https://ats-application.onrender.com/api/*` (Spring Boot API)
- **Health Check**: `https://ats-application.onrender.com/actuator/health`

### **Request Routing**
- `/` → React UI (index.html)
- `/api/*` → Spring Boot API endpoints
- `/actuator/*` → Spring Boot Actuator endpoints
- All other routes → React UI (for client-side routing)

## 🔧 **Build Process Explained**

1. **UI Build**: `cd ats-ui && npm install && npm run build`
2. **Copy UI**: Built files copied to `src/main/resources/static/`
3. **Backend Build**: `mvn clean package -DskipTests`
4. **Deploy**: Single JAR with both UI and backend

## 🎯 **Benefits of Single Service**

✅ **One URL** - Everything accessible from one domain  
✅ **No CORS issues** - UI and API on same origin  
✅ **Simpler deployment** - One service to manage  
✅ **Cost effective** - Uses only one Render service  
✅ **Easier maintenance** - Single deployment pipeline  

## 🛠️ **Troubleshooting**

### **Build Fails**
- Check if Node.js is available in Render's build environment
- Verify all dependencies are in `package.json`
- Check build logs in Render dashboard

### **UI Not Loading**
- Verify `src/main/resources/static/index.html` exists after build
- Check if `UIController.java` is properly configured
- Ensure static resources are being served

### **API Not Working**
- Check if API endpoints are accessible at `/api/*`
- Verify CORS configuration
- Check application logs

## 📊 **Expected Deployment Time**

- **Build Time**: 5-10 minutes (includes npm install + build)
- **Deploy Time**: 2-3 minutes
- **Total Time**: 10-15 minutes

## 🎉 **After Deployment**

Your complete ATS application will be available at:
- **Main Interface**: `https://ats-application.onrender.com`
- **API Health**: `https://ats-application.onrender.com/actuator/health`

Users can upload resumes, analyze them, and get AI-powered feedback all from one URL!
