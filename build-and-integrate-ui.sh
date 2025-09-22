#!/bin/bash

# Script to build React UI and integrate with Spring Boot backend

echo "🔨 Building React UI..."
cd ats-ui

# Install dependencies
npm install

# Build for production
npm run build

echo "📦 Copying built UI to Spring Boot static resources..."
cd ..

# Create static directory if it doesn't exist
mkdir -p src/main/resources/static

# Copy built React app to Spring Boot static resources
cp -r ats-ui/build/* src/main/resources/static/

echo "✅ UI integration complete!"
echo "The React UI is now integrated with the Spring Boot backend."
echo "You can deploy as a single service on Render."
