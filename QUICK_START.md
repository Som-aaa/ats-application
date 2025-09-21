# 🚀 Quick Start Guide - API Testing

## 🔑 **First: Set Up Your OpenAI API Key**

**IMPORTANT**: You need a valid OpenAI API key for the application to work!

### **Option 1: Use the Setup Script (Recommended)**
1. Run the PowerShell script: `.\setup-api-key.ps1`
2. Enter your OpenAI API key when prompted
3. The script will set the environment variable automatically

### **Option 2: Manual Setup**
1. Get your API key from: https://platform.openai.com/api-keys
2. Set environment variable: `setx OPENAI_API_KEY "your-actual-api-key"`
3. Restart your terminal/command prompt

### **Option 3: Direct Configuration**
1. Edit `src/main/resources/application.properties`
2. Replace `your-openai-api-key-here` with your actual API key
3. Restart the backend application

## ✅ **Both Servers Should Now Be Running**

### **Backend (Spring Boot)**
- **URL**: http://localhost:8080
- **Status**: Should be running with Maven wrapper

### **Frontend (React)**
- **URL**: http://localhost:3000
- **Status**: Should be running with npm start

## 🔧 **Test Your API**

### **Option 1: Use the Test Page**
1. Open `test-api.html` in your browser
2. It will automatically test all endpoints
3. Look for green ✅ success messages

### **Option 2: Manual Testing**

#### **1. Health Check**
Visit: http://localhost:8080/api/health
Expected response:
```json
{
  "status": "UP",
  "message": "ATS Application is running successfully!",
  "model": "gpt-3.5-turbo (Cost Optimized)",
  "timestamp": 1234567890
}
```

#### **2. Configuration Check**
Visit: http://localhost:8080/api/config
Expected response:
```json
{
  "model": "gpt-3.5-turbo",
  "maxTokens": 1500,
  "temperature": 0.1,
  "rateLimit": "10 requests/hour",
  "cacheEnabled": true,
  "cacheDuration": "24 hours"
}
```

#### **3. Cache Status**
Visit: http://localhost:8080/api/cache-status
Expected response:
```json
{
  "cacheEnabled": true,
  "cacheSize": 0,
  "cacheDuration": "24 hours",
  "timestampCount": 0
}
```

#### **4. Clear Cache**
Visit: http://localhost:8080/api/clear-cache (POST request)
Expected response:
```json
{
  "message": "Cache cleared successfully"
}
```

## 🎯 **If API is Not Responding**

### **Check Backend Status:**
1. Look for Spring Boot startup logs
2. Should see: "Started AtsApplication in X seconds"
3. Check if port 8080 is free: `netstat -ano | findstr :8080`

### **Check Frontend Status:**
1. Look for React startup logs
2. Should see: "Local: http://localhost:3000"
3. Check if port 3000 is free: `netstat -ano | findstr :3000`

### **Common Issues:**

#### **Port Already in Use:**
```bash
# Kill process on port 8080
netstat -ano | findstr :8080
taskkill /PID <process_id> /F

# Kill process on port 3000
netstat -ano | findstr :3000
taskkill /PID <process_id> /F
```

#### **Maven Not Found:**
- Use the Maven wrapper: `./mvnw spring-boot:run`
- Or install Maven: https://maven.apache.org/download.cgi

#### **Node.js Not Found:**
- Install Node.js: https://nodejs.org/
- Then run: `npm install` and `npm start`

## 🎉 **Success Indicators**

✅ Backend responds to http://localhost:8080/api/health  
✅ Frontend loads at http://localhost:3000  
✅ Test page shows all green checkmarks  
✅ Can upload resume and get results  
✅ Console shows debug messages  

## 🚨 **If Still Not Working**

### **Common Issues:**

#### **1. API Key Issues:**
- **Error**: "Error communicating with OpenAI"
- **Solution**: Make sure your API key is valid and properly set
- **Check**: Run `echo $env:OPENAI_API_KEY` to verify the environment variable

#### **2. Rate Limiting:**
- **Error**: "Rate limit exceeded"
- **Solution**: Wait for the hourly limit to reset or use a different client ID

#### **3. File Upload Issues:**
- **Error**: "Resume file is required"
- **Solution**: Make sure you're uploading a valid PDF or DOCX file

#### **4. General Troubleshooting:**
1. **Restart both servers**
2. **Check console for error messages**
3. **Verify API key in application.properties**
4. **Try the test page: test-api.html**
5. **Check browser console for JavaScript errors**

---

**Your API should now be working! Test it and let me know the results! 🎯** 