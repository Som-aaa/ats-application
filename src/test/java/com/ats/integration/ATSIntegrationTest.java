package com.ats.integration;

import com.ats.controller.ATSController;
import com.ats.service.ATSService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class ATSIntegrationTest {

    @Autowired
    private ATSController atsController;

    @MockBean
    private ATSService atsService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(atsController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testMode1Integration_Success() throws Exception {
        // Arrange
        MockMultipartFile resumeFile = new MockMultipartFile(
            "resume",
            "test-resume.pdf",
            "application/pdf",
            "Test resume content".getBytes()
        );

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("careerSummary", "Experienced software engineer");
        mockResponse.put("atsScore", 85);
        mockResponse.put("strengths", new String[]{"Strong technical skills"});
        mockResponse.put("weaknesses", new String[]{"Limited leadership experience"});

        when(atsService.evaluateResumeMode1(any())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/mode1")
                .file(resumeFile)
                .param("clientId", "test-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerSummary").value("Experienced software engineer"))
                .andExpect(jsonPath("$.atsScore").value(85))
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.weaknesses").isArray());
    }

    @Test
    void testMode2Integration_Success() throws Exception {
        // Arrange
        MockMultipartFile resumeFile = new MockMultipartFile(
            "resume",
            "test-resume.pdf",
            "application/pdf",
            "Test resume content".getBytes()
        );

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("careerSummary", "Experienced software engineer");
        mockResponse.put("atsScore", 90);
        mockResponse.put("matchPercentage", 85.5);

        when(atsService.evaluateResumeWithJDText(any(), anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/mode2")
                .file(resumeFile)
                .param("jd", "Looking for a software engineer with Java experience")
                .param("clientId", "test-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerSummary").value("Experienced software engineer"))
                .andExpect(jsonPath("$.atsScore").value(90))
                .andExpect(jsonPath("$.matchPercentage").value(85.5));
    }

    @Test
    void testMode3Integration_Success() throws Exception {
        // Arrange
        MockMultipartFile[] resumeFiles = {
            new MockMultipartFile("resume1", "test1.pdf", "application/pdf", "Content1".getBytes()),
            new MockMultipartFile("resume2", "test2.pdf", "application/pdf", "Content2".getBytes())
        };

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("resumeResults", new Object[]{});
        mockResponse.put("summary", new HashMap<>());

        when(atsService.bulkResumeAnalysis(any(), anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/mode3")
                .file("resumes", resumeFiles[0].getBytes())
                .file("resumes", resumeFiles[1].getBytes())
                .param("jd", "Looking for software engineers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeResults").isArray())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void testHealthCheckIntegration() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/health/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/api/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").exists());

        mockMvc.perform(get("/api/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").exists());
    }

    @Test
    void testRateLimitIntegration() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/rate-limit-status")
                .param("clientId", "test-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.used").exists())
                .andExpect(jsonPath("$.limit").exists())
                .andExpect(jsonPath("$.remaining").exists());
    }


    @Test
    void testErrorHandlingIntegration() throws Exception {
        // Test with invalid file type
        MockMultipartFile invalidFile = new MockMultipartFile(
            "resume",
            "test.txt",
            "text/plain",
            "Invalid content".getBytes()
        );

        mockMvc.perform(multipart("/api/mode1")
                .file(invalidFile))
                .andExpect(status().isBadRequest());

        // Test with missing required parameter
        mockMvc.perform(multipart("/api/mode2")
                .file(invalidFile)
                .param("jd", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCORSIntegration() throws Exception {
        // Test CORS headers
        mockMvc.perform(get("/api/health/status")
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
