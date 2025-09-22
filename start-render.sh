#!/bin/bash

# Render startup script for ATS Application
echo "Starting ATS Application on Render..."

# Set default port if not provided
export PORT=${PORT:-10000}

# Set JVM options optimized for Render free tier
export JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -Djava.security.egd=file:/dev/./urandom"

# Start the application
echo "Starting with PORT=$PORT and JAVA_OPTS=$JAVA_OPTS"
java $JAVA_OPTS -Dserver.port=$PORT -jar target/ATS-0.0.1-SNAPSHOT.jar
