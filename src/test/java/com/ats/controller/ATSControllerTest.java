package com.ats.controller;

import com.ats.service.ATSService;
import com.ats.utils.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ATSController.class)
class ATSControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ATSService atsService;

    @MockBean
    private ValidationUtils validationUtils;

    private MockMultipartFile mockResumeFile;
    private Map<String, Object> mockResponse;

    @BeforeEach
    void setUp() {
        mockResumeFile = new MockMultipartFile(
            "resume",
            "test-resume.pdf",
            "application/pdf",
            "Test resume content".getBytes()
        );

        mockResponse = new HashMap<>();
        mockResponse.put("careerSummary", "Test summary");
        mockResponse.put("atsScore", 85);
        mockResponse.put("strengths", new String[]{"Strong technical skills"});
        mockResponse.put("weaknesses", new String[]{"Limited experience"});
    }

    @Test
    void testMode1Endpoint_Success() throws Exception {
        when(atsService.evaluateResumeMode1(any())).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/mode1")
                .file(mockResumeFile)
                .param("clientId", "test-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerSummary").value("Test summary"))
                .andExpect(jsonPath("$.atsScore").value(85));
    }

    @Test
    void testMode1Endpoint_InvalidFile() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
            "resume",
            "test.txt",
            "text/plain",
            "Invalid content".getBytes()
        );

        mockMvc.perform(multipart("/api/mode1")
                .file(invalidFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMode2Endpoint_Success() throws Exception {
        when(atsService.evaluateResumeWithJDText(any(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/mode2")
                .file(mockResumeFile)
                .param("jd", "Test job description")
                .param("clientId", "test-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerSummary").value("Test summary"));
    }

    @Test
    void testMode2Endpoint_EmptyJobDescription() throws Exception {
        mockMvc.perform(multipart("/api/mode2")
                .file(mockResumeFile)
                .param("jd", "")
                .param("clientId", "test-client"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMode3Endpoint_Success() throws Exception {
        MockMultipartFile[] files = {mockResumeFile, mockResumeFile};
        when(atsService.bulkResumeAnalysis(any(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/mode3")
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .param("jd", "Test job description"))
                .andExpect(status().isOk());
    }

    @Test
    void testMode3Endpoint_TooManyFiles() throws Exception {
        MockMultipartFile[] files = new MockMultipartFile[11];
        for (int i = 0; i < 11; i++) {
            files[i] = mockResumeFile;
        }

        mockMvc.perform(multipart("/api/mode3")
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .file("resumes", mockResumeFile.getBytes())
                .param("jd", "Test job description"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRateLimitStatus() throws Exception {
        mockMvc.perform(get("/api/rate-limit-status")
                .param("clientId", "test-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.used").exists())
                .andExpect(jsonPath("$.limit").exists())
                .andExpect(jsonPath("$.remaining").exists());
    }

    @Test
    void testHealthEndpoints() throws Exception {
        mockMvc.perform(get("/api/health/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        mockMvc.perform(get("/api/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").exists());

        mockMvc.perform(get("/api/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").exists());
    }

}
