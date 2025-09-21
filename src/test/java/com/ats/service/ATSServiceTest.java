package com.ats.service;

import com.ats.exception.ATSServiceException;
import com.ats.exception.FileProcessingException;
import com.ats.exception.OpenAIException;
import com.ats.utils.FileUtils;
import com.ats.utils.OpenAIUtils;
import com.ats.utils.PromptUtils;
import com.ats.utils.ApiKeyReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ATSServiceTest {

    @Mock
    private FileUtils fileUtils;

    @Mock
    private OpenAIUtils openAIUtils;

    @Mock
    private PromptUtils promptUtils;

    @Mock
    private ApiKeyReader apiKeyReader;

    @Mock
    private ResumeMatchManager resumeMatchManager;

    @InjectMocks
    private ATSService atsService;

    private MockMultipartFile mockResumeFile;
    private String mockResumeText;
    private String mockOpenAIResponse;

    @BeforeEach
    void setUp() {
        mockResumeFile = new MockMultipartFile(
            "resume",
            "test-resume.pdf",
            "application/pdf",
            "Test resume content".getBytes()
        );

        mockResumeText = "John Doe\nSoftware Engineer\n5 years experience\nJava, Spring Boot, React";
        mockOpenAIResponse = "1. Career Summary: Experienced software engineer\n2. ATS Score: 85\n3. Strengths: Strong technical skills\n4. Weaknesses: Limited leadership experience";
    }

    @Test
    void testEvaluateResumeMode1_Success() throws Exception {
        // Arrange
        when(FileUtils.extractText(any())).thenReturn(mockResumeText);
        when(apiKeyReader.readApiKey()).thenReturn("test-api-key");
        when(promptUtils.buildMode1Prompt(anyString())).thenReturn("Test prompt");
        when(openAIUtils.callOpenAI(anyString(), anyString())).thenReturn(mockOpenAIResponse);

        // Act
        Map<String, Object> result = atsService.evaluateResumeMode1(mockResumeFile);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("careerSummary"));
        assertTrue(result.containsKey("atsScore"));
        verify(FileUtils.class, times(1));
        FileUtils.extractText(any());
        verify(openAIUtils).callOpenAI(anyString(), anyString());
    }

    @Test
    void testEvaluateResumeMode1_FileProcessingError() throws Exception {
        // Arrange
        when(FileUtils.extractText(any())).thenThrow(new FileProcessingException("File processing failed"));

        // Act & Assert
        assertThrows(ATSServiceException.class, () -> {
            atsService.evaluateResumeMode1(mockResumeFile);
        });
    }

    @Test
    void testEvaluateResumeMode1_OpenAIError() throws Exception {
        // Arrange
        when(FileUtils.extractText(any())).thenReturn(mockResumeText);
        when(apiKeyReader.readApiKey()).thenReturn("test-api-key");
        when(promptUtils.buildMode1Prompt(anyString())).thenReturn("Test prompt");
        when(openAIUtils.callOpenAI(anyString(), anyString())).thenThrow(new OpenAIException("OpenAI API error"));

        // Act & Assert
        assertThrows(ATSServiceException.class, () -> {
            atsService.evaluateResumeMode1(mockResumeFile);
        });
    }

    @Test
    void testEvaluateResumeWithJDText_Success() throws Exception {
        // Arrange
        String jdText = "Looking for a software engineer with Java experience";
        when(FileUtils.extractText(any())).thenReturn(mockResumeText);
        when(apiKeyReader.readApiKey()).thenReturn("test-api-key");
        when(promptUtils.buildMode2Prompt(anyString(), anyString())).thenReturn("Test prompt");
        when(openAIUtils.callOpenAI(anyString(), anyString())).thenReturn(mockOpenAIResponse);

        // Act
        Map<String, Object> result = atsService.evaluateResumeWithJDText(mockResumeFile, jdText);

        // Assert
        assertNotNull(result);
        verify(FileUtils.class, times(1));
        FileUtils.extractText(any());
        verify(openAIUtils).callOpenAI(anyString(), anyString());
    }

    @Test
    void testEvaluateResumeWithJDText_EmptyJobDescription() throws Exception {
        // Arrange
        String emptyJdText = "";

        // Act & Assert
        assertThrows(ATSServiceException.class, () -> {
            atsService.evaluateResumeWithJDText(mockResumeFile, emptyJdText);
        });
    }

    @Test
    void testBulkResumeAnalysis_Success() throws Exception {
        // Arrange
        MockMultipartFile[] files = {mockResumeFile, mockResumeFile};
        String jdText = "Looking for software engineers";
        when(FileUtils.extractText(any())).thenReturn(mockResumeText);
        when(apiKeyReader.readApiKey()).thenReturn("test-api-key");
        when(promptUtils.buildMode2Prompt(anyString(), anyString())).thenReturn("Test prompt");
        when(openAIUtils.callOpenAI(anyString(), anyString())).thenReturn(mockOpenAIResponse);

        // Act
        Map<String, Object> result = atsService.bulkResumeAnalysis(files, jdText);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("resumeResults"));
        verify(FileUtils.class, times(2));
        FileUtils.extractText(any());
    }

    @Test
    void testBulkResumeAnalysis_EmptyFiles() throws Exception {
        // Arrange
        MockMultipartFile[] emptyFiles = {};

        // Act & Assert
        assertThrows(ATSServiceException.class, () -> {
            atsService.bulkResumeAnalysis(emptyFiles, "Test JD");
        });
    }

    @Test
    void testGetCacheStatus() {
        // Act
        Map<String, Object> status = atsService.getCacheStatus();

        // Assert
        assertNotNull(status);
        assertTrue(status.containsKey("enabled"));
        assertTrue(status.containsKey("cacheSize"));
        assertTrue(status.containsKey("timestampCount"));
    }

    @Test
    void testClearCache() {
        // Act
        atsService.clearCache();

        // Assert - No exception should be thrown
        assertTrue(true);
    }

    @Test
    void testGetApiKeyForHealthCheck() {
        // Arrange
        when(apiKeyReader.readApiKey()).thenReturn("test-api-key");

        // Act
        String apiKey = atsService.getApiKeyForHealthCheck();

        // Assert
        assertEquals("test-api-key", apiKey);
    }

    @Test
    void testGetApiKeyForHealthCheck_FromEnvironment() {
        // Arrange
        when(apiKeyReader.readApiKey()).thenReturn(null);
        System.setProperty("OPENAI_API_KEY", "env-api-key");

        // Act
        String apiKey = atsService.getApiKeyForHealthCheck();

        // Assert
        assertEquals("env-api-key", apiKey);

        // Cleanup
        System.clearProperty("OPENAI_API_KEY");
    }
}
