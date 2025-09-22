#!/bin/bash

# Script to integrate React UI with Spring Boot backend for single deployment

echo "🔨 Building React UI for production..."

# Navigate to UI directory
cd ats-ui

# Install dependencies
echo "📦 Installing npm dependencies..."
npm install

# Build for production
echo "🏗️ Building React app..."
npm run build

# Go back to root directory
cd ..

# Create static directory if it doesn't exist
echo "📁 Creating static resources directory..."
mkdir -p src/main/resources/static

# Copy built React app to Spring Boot static resources
echo "📋 Copying built UI to Spring Boot static resources..."
cp -r ats-ui/build/* src/main/resources/static/

echo "✅ UI integration complete!"
echo "The React UI is now integrated with the Spring Boot backend."
echo "You can now deploy as a single service on Render."
