package com.ats.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // Set max file size to 10MB per file
        factory.setMaxFileSize(DataSize.ofMegabytes(10));
        
        // Set max request size to 200MB (to handle 10+ files)
        factory.setMaxRequestSize(DataSize.ofMegabytes(200));
        
        // Set file size threshold to 2MB (files larger than this will be written to disk)
        factory.setFileSizeThreshold(DataSize.ofMegabytes(2));
        
        // Set location for temporary files
        factory.setLocation(System.getProperty("java.io.tmpdir"));
        
        return factory.createMultipartConfig();
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
