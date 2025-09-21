package com.ats.performance;

import com.ats.service.ATSService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ATSPerformanceTest {

    @Mock
    private ATSService atsService;

    @Test
    void testConcurrentRequests() throws Exception {
        // Arrange
        MockMultipartFile resumeFile = new MockMultipartFile(
            "resume",
            "test-resume.pdf",
            "application/pdf",
            "Test resume content".getBytes()
        );

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("careerSummary", "Test summary");
        mockResponse.put("atsScore", 85);

        when(atsService.evaluateResumeMode1(any())).thenReturn(mockResponse);

        int numberOfThreads = 10;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // Act
        long startTime = System.currentTimeMillis();

        CompletableFuture<Void>[] futures = new CompletableFuture[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        atsService.evaluateResumeMode1(resumeFile);
                    } catch (Exception e) {
                        fail("Request failed: " + e.getMessage());
                    }
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Assert
        assertTrue(totalTime < 30000, "All requests should complete within 30 seconds");
        assertEquals(numberOfThreads * requestsPerThread, futures.length * requestsPerThread);

        executor.shutdown();
    }

    @Test
    void testBulkProcessingPerformance() throws Exception {
        // Arrange
        MockMultipartFile[] files = new MockMultipartFile[10];
        for (int i = 0; i < 10; i++) {
            files[i] = new MockMultipartFile(
                "resume" + i,
                "test" + i + ".pdf",
                "application/pdf",
                ("Test resume content " + i).getBytes()
            );
        }

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("resumeResults", new Object[]{});
        mockResponse.put("summary", new HashMap<>());

        when(atsService.bulkResumeAnalysis(any(), anyString())).thenReturn(mockResponse);

        // Act
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = atsService.bulkResumeAnalysis(files, "Test job description");
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertTrue(endTime - startTime < 10000, "Bulk processing should complete within 10 seconds");
    }

    @Test
    void testMemoryUsage() throws Exception {
        // Arrange
        MockMultipartFile largeFile = new MockMultipartFile(
            "resume",
            "large-resume.pdf",
            "application/pdf",
            new byte[1024 * 1024] // 1MB file
        );

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("careerSummary", "Test summary");
        mockResponse.put("atsScore", 85);

        when(atsService.evaluateResumeMode1(any())).thenReturn(mockResponse);

        // Act
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < 100; i++) {
            atsService.evaluateResumeMode1(largeFile);
        }

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Assert
        assertTrue(memoryUsed < 100 * 1024 * 1024, "Memory usage should be reasonable (< 100MB for 100 requests)");
    }

    @Test
    void testResponseTimeConsistency() throws Exception {
        // Arrange
        MockMultipartFile resumeFile = new MockMultipartFile(
            "resume",
            "test-resume.pdf",
            "application/pdf",
            "Test resume content".getBytes()
        );

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("careerSummary", "Test summary");
        mockResponse.put("atsScore", 85);

        when(atsService.evaluateResumeMode1(any())).thenReturn(mockResponse);

        int numberOfRequests = 20;
        long[] responseTimes = new long[numberOfRequests];

        // Act
        for (int i = 0; i < numberOfRequests; i++) {
            long startTime = System.currentTimeMillis();
            atsService.evaluateResumeMode1(resumeFile);
            long endTime = System.currentTimeMillis();
            responseTimes[i] = endTime - startTime;
        }

        // Calculate statistics
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        for (long time : responseTimes) {
            totalTime += time;
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);
        }

        long averageTime = totalTime / numberOfRequests;
        long timeVariance = maxTime - minTime;

        // Assert
        assertTrue(averageTime < 1000, "Average response time should be less than 1 second");
        assertTrue(timeVariance < 2000, "Response time variance should be reasonable");
        assertTrue(maxTime < 5000, "Maximum response time should be less than 5 seconds");
    }
}
