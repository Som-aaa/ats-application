# Deploy Both UI and Backend on Render

## 🎯 **Two-Service Deployment Strategy**

You'll deploy **2 separate services** on Render:
1. **Backend Service**: `ats-application` (Spring Boot API)
2. **Frontend Service**: `ats-ui` (React Static Site)

## 🚀 **Step-by-Step Deployment**

### **Step 1: Deploy Backend (API)**

1. **Push backend code to GitHub:**
   ```bash
   git add .
   git commit -m "Add Render deployment configuration"
   git push origin main
   ```

2. **Create Backend Service on Render:**
   - Go to [render.com](https://render.com)
   - Click **"New +"** → **"Web Service"**
   - Connect your GitHub repository
   - Configure:
     - **Name**: `ats-application`
     - **Environment**: `Java`
     - **Build Command**: `mvn clean package -DskipTests`
     - **Start Command**: `./start-render.sh`
     - **Plan**: `Free`

3. **Set Environment Variables:**
   - `SPRING_PROFILES_ACTIVE` = `production`
   - `OPENAI_API_KEY` = `your-openai-api-key`

4. **Deploy** - Wait for completion
   - Your API will be at: `https://ats-application.onrender.com`

### **Step 2: Deploy Frontend (UI)**

1. **Create Frontend Service on Render:**
   - Click **"New +"** → **"Static Site"**
   - Connect your GitHub repository
   - Configure:
     - **Name**: `ats-ui`
     - **Build Command**: `cd ats-ui && npm install && npm run build`
     - **Publish Directory**: `ats-ui/build`
     - **Plan**: `Free`

2. **Set Environment Variables:**
   - `REACT_APP_API_URL` = `https://ats-application.onrender.com`

3. **Deploy** - Wait for completion
   - Your UI will be at: `https://ats-ui.onrender.com`

## 🔗 **Connect Frontend to Backend**

### **Update CORS Configuration**

The backend needs to allow requests from your frontend domain:

1. **Update `AtsApplication.java`:**
   ```java
   @Override
   public void addCorsMappings(CorsRegistry registry) {
       registry.addMapping("/api/**")
           .allowedOrigins("http://localhost:3000", "https://ats-ui.onrender.com")
           .allowedMethods("POST", "GET", "OPTIONS");
   }
   ```

2. **Update `application-production.properties`:**
   ```properties
   app.cors.allowed-origins=https://ats-ui.onrender.com,http://localhost:3000
   ```

## 🌐 **Final URLs**

After deployment:
- **Frontend (UI)**: `https://ats-ui.onrender.com`
- **Backend (API)**: `https://ats-application.onrender.com`
- **API Health**: `https://ats-application.onrender.com/actuator/health`

## 🔄 **Alternative: Single Service Deployment**

If you prefer to deploy everything as one service, I can help you:
1. Build the React app and copy it to Spring Boot's static resources
2. Serve the UI from the same domain as the API
3. Deploy as a single Java service

**Which approach would you prefer?**
