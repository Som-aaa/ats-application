# ATS Application - Production Deployment Guide

This guide provides comprehensive instructions for deploying the ATS (Applicant Tracking System) application to production.

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose installed
- OpenAI API key configured
- Minimum 4GB RAM and 2 CPU cores
- 10GB free disk space

### One-Command Deployment

```bash
# Set your OpenAI API key
export OPENAI_API_KEY="your-api-key-here"

# Deploy the application
./deploy.sh deploy
```

## 📋 Detailed Setup

### 1. Environment Configuration

#### Option A: Environment Variable
```bash
export OPENAI_API_KEY="sk-your-openai-api-key-here"
```

#### Option B: API Key File
Create `api-key.txt` in the project root:
```
sk-your-openai-api-key-here
```

### 2. Production Configuration

The application uses Spring profiles for environment-specific configuration:

- **Development**: `application.properties`
- **Production**: `application-production.properties`

Key production settings:
- Enhanced logging with log rotation
- Optimized JVM settings
- Security headers
- Rate limiting
- Health checks

### 3. Deployment Options

#### Option A: Docker Compose (Recommended)

```bash
# Deploy with Docker Compose
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

#### Option B: Docker Run

```bash
# Build the image
docker build -t ats-app .

# Run the container
docker run -d \
  --name ats-container \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e OPENAI_API_KEY="$OPENAI_API_KEY" \
  -v $(pwd)/logs:/app/logs \
  ats-app
```

#### Option C: Manual Deployment

```bash
# Build the application
mvn clean package -DskipTests

# Run with production profile
java -jar target/ATS-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

## 🔧 Configuration

### Application Properties

Key configuration options in `application-production.properties`:

```properties
# Server Configuration
server.port=8080
server.compression.enabled=true

# File Upload Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=100MB

# Rate Limiting
app.rate-limit.max-requests-per-hour=50

# OpenAI Configuration
openai.timeout=30000
openai.retry-attempts=3

# Logging
logging.file.name=logs/ats-application.log
logging.file.max-size=50MB
logging.file.max-history=30
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key | Required |
| `SPRING_PROFILES_ACTIVE` | Spring profile | production |
| `JAVA_OPTS` | JVM options | -Xms512m -Xmx2g |

## 📊 Monitoring and Health Checks

### Health Check Endpoints

- **Liveness**: `GET /api/health/liveness`
- **Readiness**: `GET /api/health/ready`
- **Status**: `GET /api/health/status`

### Metrics

- **Prometheus**: `GET /actuator/prometheus`
- **General Metrics**: `GET /actuator/metrics`

### Logging

Logs are written to:
- Console (for container logs)
- File: `logs/ats-application.log`

Log levels:
- `INFO`: General application events
- `WARN`: Warning conditions
- `ERROR`: Error conditions
- `DEBUG`: Detailed debugging information

## 🔒 Security Considerations

### Production Security Features

1. **Input Validation**: All inputs are validated and sanitized
2. **Rate Limiting**: API rate limiting to prevent abuse
3. **Error Handling**: Secure error messages without sensitive information
4. **CORS Configuration**: Configurable CORS settings
5. **File Upload Security**: File type and size validation

### Security Headers

The application includes security headers:
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`
- `Referrer-Policy: strict-origin-when-cross-origin`

### API Key Security

- Store API keys in environment variables or secure files
- Never commit API keys to version control
- Use different API keys for different environments

## 🌐 Reverse Proxy Setup (Nginx)

For production deployments, use Nginx as a reverse proxy:

```bash
# Copy nginx configuration
cp nginx.conf /etc/nginx/nginx.conf

# Test configuration
nginx -t

# Reload nginx
systemctl reload nginx
```

### Nginx Features

- SSL/TLS termination
- Rate limiting
- Gzip compression
- Security headers
- Load balancing (if multiple instances)

## 📈 Performance Optimization

### JVM Tuning

Default JVM options for production:
```bash
JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication"
```

### Database Optimization

If using a database in the future:
- Connection pooling
- Query optimization
- Indexing strategy
- Caching layer

### Caching

The application includes:
- Response caching for API calls
- File processing caching
- Configurable cache duration

## 🚨 Troubleshooting

### Common Issues

1. **Application won't start**
   - Check API key configuration
   - Verify port 8080 is available
   - Check logs for errors

2. **File upload failures**
   - Verify file size limits
   - Check file type restrictions
   - Ensure sufficient disk space

3. **OpenAI API errors**
   - Verify API key is valid
   - Check API quota and limits
   - Review rate limiting settings

### Debug Commands

```bash
# Check container status
docker ps -a

# View application logs
docker logs ats-container

# Check health status
curl http://localhost:8080/api/health/status

# Test API endpoint
curl -X POST http://localhost:8080/api/mode1 \
  -F "resume=@test-resume.pdf"
```

### Log Analysis

```bash
# View recent logs
tail -f logs/ats-application.log

# Search for errors
grep "ERROR" logs/ats-application.log

# Monitor real-time logs
docker-compose logs -f
```

## 🔄 Maintenance

### Regular Tasks

1. **Log Rotation**: Automatic with logback configuration
2. **Health Monitoring**: Use health check endpoints
3. **Performance Monitoring**: Monitor metrics and logs
4. **Security Updates**: Keep dependencies updated

### Backup Strategy

1. **Application Logs**: Backup log files regularly
2. **Configuration**: Version control all configuration files
3. **API Keys**: Secure backup of API keys

### Updates

```bash
# Pull latest changes
git pull origin main

# Rebuild and redeploy
./deploy.sh build
./deploy.sh deploy
```

## 📞 Support

For production support:

1. Check application logs first
2. Verify health check endpoints
3. Review configuration settings
4. Check system resources (CPU, memory, disk)

## 🎯 Production Checklist

Before going live:

- [ ] API key configured and tested
- [ ] Health checks working
- [ ] Logging configured
- [ ] Rate limiting appropriate
- [ ] Security headers enabled
- [ ] File upload limits set
- [ ] Monitoring in place
- [ ] Backup strategy defined
- [ ] SSL/TLS configured (if needed)
- [ ] Load testing completed

## 📚 Additional Resources

- [Spring Boot Production Features](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Nginx Configuration Guide](https://nginx.org/en/docs/)
- [OpenAI API Documentation](https://platform.openai.com/docs)
