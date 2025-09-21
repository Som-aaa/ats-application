# 🔑 How to Set Your OpenAI API Key

## **Option 1: Simple File Method (Recommended)**

1. **Edit the file `api-key.txt`**
2. **Replace `your-openai-api-key-here` with your actual API key**
3. **Save the file**
4. **Restart the backend application**

Example:
```
# Paste your OpenAI API key here (without quotes)
# Example: sk-1234567890abcdef1234567890abcdef1234567890abcdef
# 
# Get your API key from: https://platform.openai.com/api-keys
# 
# After pasting your key, save this file and restart the application

sk-your-actual-api-key-here
```

## **Option 2: Using the Script**

1. **Run the script**: `.\set-api-key.ps1`
2. **The script will read your API key from `api-key.txt`**
3. **Restart your terminal and the application**

## **Option 3: Environment Variable**

1. **Set environment variable**: `setx OPENAI_API_KEY "your-api-key"`
2. **Restart your terminal**
3. **Restart the application**

## **Get Your API Key**

1. Go to: https://platform.openai.com/api-keys
2. Create a new API key
3. Copy the key (starts with `sk-`)

## **After Setting the Key**

1. **Restart the backend**: Stop (Ctrl+C) and run `./mvnw spring-boot:run`
2. **Test the application**: Open `test-api.html` in your browser
3. **Try uploading a resume**: Go to http://localhost:3000

## **Troubleshooting**

- **Error**: "OpenAI API key not configured"
  - **Solution**: Make sure you've set the API key in `api-key.txt`
  
- **Error**: "Error communicating with OpenAI"
  - **Solution**: Check if your API key is valid and has credits

- **Still not working?**
  - Check the console for error messages
  - Make sure the API key doesn't have extra spaces
  - Verify the backend is running on port 8080 