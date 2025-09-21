
package com.ats.service;

import com.ats.utils.FileUtils;
import com.ats.utils.OpenAIUtils;
import com.ats.utils.PromptUtils;
import com.ats.utils.ApiKeyReader;
import com.ats.utils.ValidationUtils;
import com.ats.exception.ATSServiceException;
import com.ats.exception.FileProcessingException;
import com.ats.exception.OpenAIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.IndexedColors;
import java.io.ByteArrayOutputStream;

@Service
public class ATSService {

    private static final Logger logger = LoggerFactory.getLogger(ATSService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;

    @Autowired
    private OpenAIUtils openAIUtils;
    
    @Autowired
    private ApiKeyReader apiKeyReader;
    
    @Autowired
    private ResumeMatchManager resumeMatchManager;

    // Cache for storing API responses (in production, use Redis or database)
    private final Map<String, Map<String, Object>> responseCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    public Map<String, Object> evaluateResumeMode1(MultipartFile resume) throws ATSServiceException {
        logger.info("Starting Mode 1 evaluation for resume: {}", resume.getOriginalFilename());
        
        try {
            // Extract text from resume
            String resumeText = FileUtils.extractText(resume);
            logger.debug("Resume text extracted successfully, length: {}", resumeText.length());
            
            String cacheKey = generateCacheKey("mode1", resumeText);
            
            // Check cache first (only if enabled)
            if (cacheEnabled) {
                Map<String, Object> cachedResult = getCachedResult(cacheKey);
                if (cachedResult != null) {
                    logger.info("Cache hit for Mode 1 - using cached result");
                    return cachedResult;
                }
            }

            // Get API key dynamically
            String currentApiKey = getApiKey();
            if (currentApiKey == null || currentApiKey.equals("your-openai-api-key-here")) {
                throw new ATSServiceException("OpenAI API key not configured. Please set your API key in api-key.txt or environment variable OPENAI_API_KEY");
            }

            // Generate new result
            logger.debug("Processing new Mode 1 request for resume length: {}", resumeText.length());
            String prompt = PromptUtils.buildMode1Prompt(resumeText);
            String openAIResponse = openAIUtils.callOpenAI(currentApiKey, prompt);
            Map<String, Object> result = parseMode1Response(openAIResponse);
            
            // Cache the result (only if enabled)
            if (cacheEnabled) {
                cacheResult(cacheKey, result);
                logger.debug("Result cached for Mode 1");
            }
            
            // Ensure all keys are present in the result for Mode 1
            result = ensureMode1ResultStructure(result);
            
            logger.info("Mode 1 evaluation completed successfully for resume: {}", resume.getOriginalFilename());
            return result;
            
        } catch (FileProcessingException e) {
            logger.error("File processing error in Mode 1 for resume: {}", resume.getOriginalFilename(), e);
            throw new ATSServiceException("Failed to process resume file", e);
        } catch (OpenAIException e) {
            logger.error("OpenAI API error in Mode 1 for resume: {}", resume.getOriginalFilename(), e);
            throw new ATSServiceException("Failed to analyze resume with AI", e);
        } catch (Exception e) {
            logger.error("Unexpected error in Mode 1 for resume: {}", resume.getOriginalFilename(), e);
            throw new ATSServiceException("Unexpected error during resume analysis", e);
        }
    }
    
    private Map<String, Object> ensureMode1ResultStructure(Map<String, Object> result) {
        if (!result.containsKey("projects")) {
            result.put("projects", Map.of("matchedSkills", "None"));
        }
        if (!result.containsKey("certificates")) {
            result.put("certificates", Map.of("matchedSkills", "None"));
        }
        if (!result.containsKey("technicalSkills")) {
            result.put("technicalSkills", Map.of("matchedSkills", "None"));
        }
        if (!result.containsKey("workExperience")) {
            result.put("workExperience", Map.of("matchedSkills", "None"));
        }
        if (!result.containsKey("strengths")) {
            result.put("strengths", new ArrayList<>());
        }
        if (!result.containsKey("weaknesses")) {
            result.put("weaknesses", new ArrayList<>());
        }
        if (!result.containsKey("careerSummary")) {
            result.put("careerSummary", new ArrayList<>());
        }
        if (!result.containsKey("suggestions")) {
            result.put("suggestions", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> evaluateResumeWithJDText(MultipartFile resume, String jdText) throws ATSServiceException {
        logger.info("Starting Mode 2 evaluation for resume: {} with JD length: {}", 
            resume.getOriginalFilename(), jdText.length());
        
        try {
            // Extract text from resume
            String resumeText = FileUtils.extractText(resume);
            logger.debug("Resume text extracted successfully, length: {}", resumeText.length());
            
            // Validate and sanitize JD text
            String sanitizedJdText = ValidationUtils.sanitizeText(jdText);
            logger.debug("JD text sanitized, length: {}", sanitizedJdText.length());
            
            String cacheKey = generateCacheKey("mode2", resumeText + "|||" + sanitizedJdText);
            
            // Check cache first (only if enabled)
            if (cacheEnabled) {
                Map<String, Object> cachedResult = getCachedResult(cacheKey);
                if (cachedResult != null) {
                    logger.info("Cache hit for Mode 2 - using cached result");
                    return cachedResult;
                }
            }

            // Get API key dynamically
            String currentApiKey = getApiKey();
            if (currentApiKey == null || currentApiKey.equals("your-openai-api-key-here")) {
                throw new ATSServiceException("OpenAI API key not configured. Please set your API key in api-key.txt or environment variable OPENAI_API_KEY");
            }

            // Generate new result
            logger.debug("Processing new Mode 2 request for resume length: {}, JD length: {}", 
                resumeText.length(), sanitizedJdText.length());
            logger.debug("Job Description (first 200 chars): {}", 
                sanitizedJdText.substring(0, Math.min(200, sanitizedJdText.length())));
            String prompt = PromptUtils.buildMode2Prompt(resumeText, sanitizedJdText);
            String openAIResponse = openAIUtils.callOpenAI(currentApiKey, prompt);
            Map<String, Object> result = parseMode2Response(openAIResponse, resumeText);
            
            // Cache the result (only if enabled)
            if (cacheEnabled) {
                cacheResult(cacheKey, result);
                logger.debug("Result cached for Mode 2");
            }
            
            logger.info("Mode 2 evaluation completed successfully for resume: {}", resume.getOriginalFilename());
            return result;
            
        } catch (FileProcessingException e) {
            logger.error("File processing error in Mode 2 for resume: {}", resume.getOriginalFilename(), e);
            throw new ATSServiceException("Failed to process resume file", e);
        } catch (OpenAIException e) {
            logger.error("OpenAI API error in Mode 2 for resume: {}", resume.getOriginalFilename(), e);
            throw new ATSServiceException("Failed to analyze resume with AI", e);
        } catch (Exception e) {
            logger.error("Unexpected error in Mode 2 for resume: {}", resume.getOriginalFilename(), e);
            throw new ATSServiceException("Unexpected error during resume analysis", e);
        }
    }

    public Map<String, Object> bulkResumeAnalysis(MultipartFile[] resumes, String jdText) {
        List<Map<String, Object>> resumeResults = new ArrayList<>();
        Map<String, Object> summary = new HashMap<>();
        
        System.out.println("DEBUG - Starting bulk analysis for " + resumes.length + " resumes");
        
        // Process each resume
        for (int i = 0; i < resumes.length; i++) {
            MultipartFile resume = resumes[i];
            try {
                System.out.println("DEBUG - Processing resume " + (i + 1) + "/" + resumes.length + ": " + resume.getOriginalFilename());
                
                // Use existing Mode 2 logic for each resume
                Map<String, Object> result = evaluateResumeWithJDText(resume, jdText);
                
                // Add resume metadata
                result.put("resumeName", resume.getOriginalFilename());
                result.put("resumeIndex", i);
                result.put("fileSize", resume.getSize());
                
                // Store original resume content for download
                try {
                    String resumeContent = FileUtils.extractText(resume);
                    result.put("originalResumeContent", resumeContent);
                    result.put("originalResumeName", resume.getOriginalFilename());
                    System.out.println("DEBUG - Stored resume content for: " + resume.getOriginalFilename() + ", Length: " + resumeContent.length());
                } catch (Exception e) {
                    System.out.println("DEBUG - Error storing resume content: " + e.getMessage());
                    e.printStackTrace();
                }
                
                resumeResults.add(result);
                
            } catch (Exception e) {
                System.out.println("DEBUG - Error processing resume " + (i + 1) + ": " + e.getMessage());
                // Add error result for this resume
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("resumeName", resume.getOriginalFilename());
                errorResult.put("resumeIndex", i);
                errorResult.put("error", "Failed to process: " + e.getMessage());
                errorResult.put("atsScore", List.of(0.0));
                resumeResults.add(errorResult);
            }
        }
        
        // Sort all resumes by ATS score (highest first) - no threshold
        List<Map<String, Object>> allValidResumes = new ArrayList<>();
        for (Map<String, Object> result : resumeResults) {
            if (!result.containsKey("error")) {
                allValidResumes.add(result);
            }
        }
        
        // Sort by ATS score (highest first)
        allValidResumes.sort((a, b) -> {
            Double scoreA = ((List<Double>) a.get("atsScore")).get(0);
            Double scoreB = ((List<Double>) b.get("atsScore")).get(0);
            return scoreB.compareTo(scoreA); // Descending order
        });
        
        // The first resume (highest score) is the matched one
        List<Map<String, Object>> matchedResumes = new ArrayList<>();
        List<Map<String, Object>> unmatchedResumes = new ArrayList<>();
        
        if (!allValidResumes.isEmpty()) {
            // First resume (highest score) is matched
            matchedResumes.add(allValidResumes.get(0));
            // Rest are unmatched
            for (int i = 1; i < allValidResumes.size(); i++) {
                unmatchedResumes.add(allValidResumes.get(i));
            }
        }
        
        // Generate new resume names for matched resumes
        for (Map<String, Object> matchedResume : matchedResumes) {
            try {
                String resumeContent = (String) matchedResume.get("originalResumeContent");
                if (resumeContent != null) {
                    // Extract company and role from the AI analysis
                    String companyName = (String) matchedResume.get("companyName");
                    String roleName = (String) matchedResume.get("roleName");
                    
                    if (companyName != null && roleName != null) {
                        String newResumeName = generateNewResumeName(companyName, roleName, resumeContent);
                        matchedResume.put("newResumeName", newResumeName);
                    }
                }
            } catch (Exception e) {
                System.out.println("DEBUG - Error generating resume name: " + e.getMessage());
            }
        }
        
        // Calculate summary statistics for matched resumes only
        double totalScore = 0;
        double highestScore = 0;
        double lowestScore = 10;
        int matchedCount = matchedResumes.size();
        
        for (Map<String, Object> result : matchedResumes) {
            Double score = ((List<Double>) result.get("atsScore")).get(0);
            totalScore += score;
            highestScore = Math.max(highestScore, score);
            lowestScore = Math.min(lowestScore, score);
        }
        
        double averageScore = matchedCount > 0 ? totalScore / matchedCount : 0;
        
        // Build summary
        summary.put("totalResumes", resumes.length);
        summary.put("matchedResumes", matchedCount);
        summary.put("unmatchedResumes", unmatchedResumes.size());
        summary.put("averageScore", Math.round(averageScore * 100.0) / 100.0);
        summary.put("highestScore", Math.round(highestScore * 100.0) / 100.0);
        summary.put("lowestScore", Math.round(lowestScore * 100.0) / 100.0);
        summary.put("bestMatch", matchedCount > 0 ? matchedResumes.get(0) : null);
        
        // Build final result
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("summary", summary);
        finalResult.put("matchedResults", matchedResumes);
        finalResult.put("unmatchedResults", unmatchedResumes);
        finalResult.put("jobDescription", jdText);
        
        if (matchedCount > 0) {
            System.out.println("DEBUG - Bulk analysis completed. Best match: " + 
                ((Map<String, Object>) matchedResumes.get(0)).get("resumeName") + 
                " with score: " + ((List<Double>) matchedResumes.get(0).get("atsScore")).get(0));
        } else {
            System.out.println("DEBUG - Bulk analysis completed. No matched resumes found.");
        }
        
        return finalResult;
    }

    public Map<String, Object> bulkJDResumeAnalysis(MultipartFile[] resumes, MultipartFile jdFile) {
        List<Map<String, Object>> allResults = new ArrayList<>();
        
        System.out.println("DEBUG - Starting bulk JD analysis for " + resumes.length + " resumes");
        
        try {
            // Parse Excel file to extract job descriptions with company/role info
            List<Map<String, String>> jobDescriptions = parseExcelJobDescriptions(jdFile);
            System.out.println("DEBUG - Extracted " + jobDescriptions.size() + " job descriptions from Excel");
            
            if (jobDescriptions.isEmpty()) {
                throw new RuntimeException("No job descriptions found in the Excel file");
            }
            
            // Process each resume against each job description
            for (int resumeIndex = 0; resumeIndex < resumes.length; resumeIndex++) {
                MultipartFile resume = resumes[resumeIndex];
                System.out.println("DEBUG - Processing resume " + (resumeIndex + 1) + "/" + resumes.length + ": " + resume.getOriginalFilename());
                
                List<Map<String, Object>> resumeResults = new ArrayList<>();
                
                for (int jdIndex = 0; jdIndex < jobDescriptions.size(); jdIndex++) {
                    Map<String, String> jobInfo = jobDescriptions.get(jdIndex);
                    String jdText = jobInfo.get("description");
                    String companyName = jobInfo.get("companyName");
                    String roleName = jobInfo.get("roleName");
                    
                    try {
                        // Use existing Mode 2 logic for each resume-JD combination
                        Map<String, Object> result = evaluateResumeWithJDText(resume, jdText);
                        
                        // Override AI-extracted company/role with Excel data (more reliable)
                        result.put("companyName", companyName);
                        result.put("roleName", roleName);
                        
                        // Add metadata
                        result.put("resumeName", resume.getOriginalFilename());
                        result.put("resumeIndex", resumeIndex);
                        result.put("jdIndex", jdIndex);
                        result.put("jdText", jdText.substring(0, Math.min(100, jdText.length())) + "...");
                        result.put("fileSize", resume.getSize());
                        
                        // Store the resume match in ResumeMatchManager
                        try {
                            System.out.println("DEBUG - Storing resume file for: " + resume.getOriginalFilename());
                            System.out.println("DEBUG - File size: " + resume.getSize() + " bytes");
                            System.out.println("DEBUG - Content type: " + resume.getContentType());
                            
                            // Store the resume match using the new file storage system
                            resumeMatchManager.storeResumeMatch(
                                jdIndex,
                                jdText,
                                resume.getOriginalFilename(),
                                resume.getOriginalFilename(),
                                ((List<Double>) result.get("atsScore")).get(0).doubleValue(),
                                resume
                            );
                            
                            System.out.println("DEBUG - Stored resume match for resume: " + resume.getOriginalFilename() + 
                                              " with JD: " + jdIndex);
                        } catch (Exception e) {
                            System.out.println("DEBUG - Error storing resume match: " + e.getMessage());
                            e.printStackTrace();
                            // Continue processing even if storage fails
                        }
                        
                        resumeResults.add(result);
                        
                    } catch (Exception e) {
                        System.out.println("DEBUG - Error processing resume " + (resumeIndex + 1) + " with JD " + (jdIndex + 1) + ": " + e.getMessage());
                        // Add error result for this combination
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("resumeName", resume.getOriginalFilename());
                        errorResult.put("resumeIndex", resumeIndex);
                        errorResult.put("jdIndex", jdIndex);
                        errorResult.put("jdText", jdText.substring(0, Math.min(100, jdText.length())) + "...");
                        errorResult.put("error", "Failed to process: " + e.getMessage());
                        errorResult.put("atsScore", List.of(0.0));
                        resumeResults.add(errorResult);
                    }
                }
                
                // Find best match for this resume
                Map<String, Object> bestMatch = findBestMatch(resumeResults);
                
                // Store original resume content for download
                if (bestMatch != null && !bestMatch.containsKey("error")) {
                    try {
                        String resumeContent = FileUtils.extractText(resumes[resumeIndex]);
                        bestMatch.put("originalResumeContent", resumeContent);
                        bestMatch.put("originalResumeName", resume.getOriginalFilename());
                    } catch (Exception e) {
                        System.out.println("DEBUG - Error storing resume content: " + e.getMessage());
                    }
                }
                
                // Generate new resume name ONLY for the best match
                if (bestMatch != null && !bestMatch.containsKey("error")) {
                    try {
                        // Extract resume content for username extraction
                        String resumeContent = FileUtils.extractText(resumes[resumeIndex]);
                        
                        // Get company and role from the best match result (which has the Excel data)
                        String companyName = (String) bestMatch.get("companyName");
                        String roleName = (String) bestMatch.get("roleName");
                        
                        // Generate new resume name using Excel company/role data and resume content
                        String newResumeName = generateNewResumeName(companyName, roleName, resumeContent);
                        bestMatch.put("newResumeName", newResumeName);
                        
                        // Also set the resume name if not already set
                        if (bestMatch.get("resumeName") == null) {
                            bestMatch.put("resumeName", resumes[resumeIndex].getOriginalFilename());
                        }
                        
                        // Debug logging
                        System.out.println("DEBUG - Resume: " + resumes[resumeIndex].getOriginalFilename());
                        System.out.println("DEBUG - Company from Excel: " + companyName);
                        System.out.println("DEBUG - Role from Excel: " + roleName);
                        System.out.println("DEBUG - Username extracted: " + extractUsernameFromResume(resumeContent));
                        System.out.println("DEBUG - Generated name: " + newResumeName);
                    } catch (Exception e) {
                        System.out.println("DEBUG - Error generating resume name for best match: " + e.getMessage());
                        bestMatch.put("newResumeName", "Error_Generating_Name");
                        // Ensure resume name is set even if generation fails
                        if (bestMatch.get("resumeName") == null) {
                            bestMatch.put("resumeName", resumes[resumeIndex].getOriginalFilename());
                        }
                    }
                }
                
                // Sort all results by ATS score (highest first) - no threshold
                List<Map<String, Object>> allValidMatches = new ArrayList<>();
                for (Map<String, Object> match : resumeResults) {
                    if (!match.containsKey("error")) {
                        allValidMatches.add(match);
                    }
                }
                
                // Sort by ATS score (highest first)
                allValidMatches.sort((a, b) -> {
                    Double scoreA = ((List<Double>) a.get("atsScore")).get(0);
                    Double scoreB = ((List<Double>) b.get("atsScore")).get(0);
                    return scoreB.compareTo(scoreA); // Descending order
                });
                
                // The first match (highest score) is matched, rest are unmatched
                List<Map<String, Object>> matchedResults = new ArrayList<>();
                List<Map<String, Object>> unmatchedResults = new ArrayList<>();
                
                if (!allValidMatches.isEmpty()) {
                    // First match (highest score) is matched
                    matchedResults.add(allValidMatches.get(0));
                    // Rest are unmatched
                    for (int i = 1; i < allValidMatches.size(); i++) {
                        unmatchedResults.add(allValidMatches.get(i));
                    }
                }
                
                // Create a clean copy of allMatches without circular references
                List<Map<String, Object>> cleanMatches = new ArrayList<>();
                for (Map<String, Object> match : resumeResults) {
                    Map<String, Object> cleanMatch = new HashMap<>(match);
                    // Remove any potential circular references
                    cleanMatch.remove("allMatches");
                    
                    // Also store the resume content in each match for download purposes
                    if (bestMatch != null && !bestMatch.containsKey("error")) {
                        try {
                            String resumeContent = FileUtils.extractText(resumes[resumeIndex]);
                            cleanMatch.put("originalResumeContent", resumeContent);
                            cleanMatch.put("originalResumeName", resume.getOriginalFilename());
                            System.out.println("DEBUG - Stored resume content in match for: " + cleanMatch.get("resumeName") + ", Content length: " + resumeContent.length());
                        } catch (Exception e) {
                            System.out.println("DEBUG - Error storing resume content in match: " + e.getMessage());
                        }
                    }
                    
                    cleanMatches.add(cleanMatch);
                }
                bestMatch.put("allMatches", cleanMatches);
                bestMatch.put("matchedResults", matchedResults);
                bestMatch.put("unmatchedResults", unmatchedResults);
                
                allResults.add(bestMatch);
            }
            
            // Calculate overall summary statistics
            Map<String, Object> summary = calculateOverallSummary(allResults, jobDescriptions.size());
            
            // Generate Excel with results
            byte[] excelData = generateExcelWithResults(jobDescriptions, allResults, resumes);
            
            // Convert byte array to base64 string for JSON transmission
            String excelDataBase64 = java.util.Base64.getEncoder().encodeToString(excelData);
            
            // Build final result
            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("summary", summary);
            finalResult.put("resumeResults", allResults);
            finalResult.put("totalJobDescriptions", jobDescriptions.size());
            finalResult.put("totalResumes", resumes.length);
            finalResult.put("excelData", excelDataBase64);
            finalResult.put("excelFileName", "JD_Analysis_Results.xlsx");
            
            // Add matched and unmatched counts
            int totalMatched = 0;
            int totalUnmatched = 0;
            for (Map<String, Object> result : allResults) {
                if (result.containsKey("matchedResults")) {
                    totalMatched += ((List<Map<String, Object>>) result.get("matchedResults")).size();
                }
                if (result.containsKey("unmatchedResults")) {
                    totalUnmatched += ((List<Map<String, Object>>) result.get("unmatchedResults")).size();
                }
            }
            finalResult.put("totalMatched", totalMatched);
            finalResult.put("totalUnmatched", totalUnmatched);
            
            System.out.println("DEBUG - Bulk JD analysis completed successfully");
            
            // Debug: Check what's being sent to frontend
            System.out.println("DEBUG - Final result structure:");
            System.out.println("  - resumeResults size: " + allResults.size());
            for (int i = 0; i < allResults.size(); i++) {
                Map<String, Object> result = allResults.get(i);
                System.out.println("  - Resume " + i + ": " + result.get("resumeName") + 
                    ", hasContent: " + result.containsKey("originalResumeContent") +
                    ", contentLength: " + (result.get("originalResumeContent") != null ? ((String) result.get("originalResumeContent")).length() : "NULL"));
                
                if (result.containsKey("allMatches")) {
                    List<Map<String, Object>> allMatches = (List<Map<String, Object>>) result.get("allMatches");
                    System.out.println("    - allMatches size: " + allMatches.size());
                    for (int j = 0; j < allMatches.size(); j++) {
                        Map<String, Object> match = allMatches.get(j);
                        System.out.println("    - Match " + j + ": " + match.get("resumeName") + 
                            ", hasContent: " + match.containsKey("originalResumeContent") +
                            ", contentLength: " + (match.get("originalResumeContent") != null ? ((String) match.get("originalResumeContent")).length() : "NULL"));
                    }
                }
            }
            
            // Validate and clean the response data
            finalResult = validateAndCleanResponse(finalResult);
            
            return finalResult;
            
        } catch (Exception e) {
            System.out.println("DEBUG - Bulk JD analysis failed: " + e.getMessage());
            throw new RuntimeException("Bulk JD analysis failed: " + e.getMessage(), e);
        }
    }

private Map<String, Object> validateAndCleanResponse(Map<String, Object> response) {
    try {
        // Create a clean copy without circular references
        Map<String, Object> cleanedResponse = new HashMap<>();
        
        // Copy summary
        if (response.containsKey("summary")) {
            Map<String, Object> summary = new HashMap<>((Map<String, Object>) response.get("summary"));
            // Remove bestOverallMatch to avoid circular reference
            summary.remove("bestOverallMatch");
            cleanedResponse.put("summary", summary);
        }
        
        // Copy other fields
        cleanedResponse.put("totalJobDescriptions", response.get("totalJobDescriptions"));
        cleanedResponse.put("totalResumes", response.get("totalResumes"));
        cleanedResponse.put("excelData", response.get("excelData"));
        cleanedResponse.put("excelFileName", response.get("excelFileName"));
        
        // Copy resume results with clean allMatches
        if (response.containsKey("resumeResults")) {
            List<Map<String, Object>> cleanResumeResults = new ArrayList<>();
            List<Map<String, Object>> resumeResults = (List<Map<String, Object>>) response.get("resumeResults");
            
            for (Map<String, Object> resumeResult : resumeResults) {
                Map<String, Object> cleanResumeResult = new HashMap<>(resumeResult);
                
                // Clean allMatches to avoid circular references
                if (cleanResumeResult.containsKey("allMatches")) {
                    List<Map<String, Object>> allMatches = (List<Map<String, Object>>) cleanResumeResult.get("allMatches");
                    List<Map<String, Object>> cleanAllMatches = new ArrayList<>();
                    
                    for (Map<String, Object> match : allMatches) {
                        Map<String, Object> cleanMatch = new HashMap<>(match);
                        cleanMatch.remove("allMatches"); // Remove circular reference
                        cleanAllMatches.add(cleanMatch);
                    }
                    
                    cleanResumeResult.put("allMatches", cleanAllMatches);
                }
                
                cleanResumeResults.add(cleanResumeResult);
            }
            
            cleanedResponse.put("resumeResults", cleanResumeResults);
        }
        
        return cleanedResponse;
        
    } catch (Exception e) {
        System.out.println("DEBUG - Error cleaning response: " + e.getMessage());
        // Return a safe fallback response
        Map<String, Object> safeResponse = new HashMap<>();
        safeResponse.put("error", "Response processing error");
        safeResponse.put("summary", Map.of(
            "totalResumes", 0,
            "totalJobDescriptions", 0,
            "validResults", 0,
            "averageScore", 0.0,
            "highestScore", 0.0,
            "lowestScore", 0.0
        ));
        safeResponse.put("resumeResults", new ArrayList<>());
        return safeResponse;
    }
}

    private List<Map<String, String>> parseExcelJobDescriptions(MultipartFile file) throws IOException {
        List<Map<String, String>> jobDescriptions = new ArrayList<>();
        
        try (InputStream is = file.getInputStream()) {
            Workbook workbook;
            
            if (file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(is);
            } else {
                workbook = new HSSFWorkbook(is);
            }
            
            Sheet sheet = workbook.getSheetAt(0); // Get first sheet
            
            // Validate sheet structure
            validateExcelStructure(sheet);
            
            // Check if sheet is empty
            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new RuntimeException("Excel file appears to be empty or contains only header row. Please ensure you have data in the following format:\n" +
                    "Column A: Company Name\n" +
                    "Column B: Job Role/Title\n" +
                    "Column C: Job Description\n" +
                    "Each row should contain one job posting.");
            }
            
            // Auto-detect column positions based on headers
            Row headerRow = sheet.getRow(0);
            int companyColIndex = -1;
            int roleColIndex = -1;
            int descriptionColIndex = -1;
            int applyLinkColIndex = -1;
            
            // Find columns by analyzing header names
            for (int colIndex = 0; colIndex < headerRow.getLastCellNum(); colIndex++) {
                Cell headerCell = headerRow.getCell(colIndex);
                if (headerCell != null) {
                    String cellValue = getCellValueAsString(headerCell);
                    String headerText = (cellValue != null) ? cellValue.toLowerCase().trim() : "";
                    
                    // Look for company-related headers
                    if (companyColIndex == -1 && (headerText.contains("company") || 
                                                  headerText.contains("organization") || 
                                                  headerText.contains("employer") ||
                                                  headerText.contains("firm") ||
                                                  headerText.equals("company_name"))) {
                        companyColIndex = colIndex;
                        System.out.println("DEBUG - Found Company column at index " + colIndex + ": '" + headerText + "'");
                    }
                    
                    // Look for role-related headers
                    else if (roleColIndex == -1 && (headerText.contains("role") || 
                                                   headerText.contains("title") || 
                                                   headerText.contains("position") ||
                                                   headerText.contains("job") ||
                                                   headerText.contains("designation"))) {
                        roleColIndex = colIndex;
                        System.out.println("DEBUG - Found Role column at index " + colIndex + ": '" + headerText + "'");
                    }
                    
                    // Look for apply link headers
                    else if (applyLinkColIndex == -1 && (headerText.contains("apply") || 
                                                        headerText.contains("link") || 
                                                        headerText.contains("url") ||
                                                        headerText.contains("application") ||
                                                        headerText.contains("careers") ||
                                                        headerText.equals("apply_link"))) {
                        applyLinkColIndex = colIndex;
                        System.out.println("DEBUG - Found Apply Link column at index " + colIndex + ": '" + headerText + "'");
                    }
                    
                    // Look for description-related headers
                    else if (descriptionColIndex == -1 && (headerText.contains("description") || 
                                                         headerText.contains("requirements") || 
                                                         headerText.contains("responsibilities") ||
                                                         headerText.contains("duties") ||
                                                         headerText.contains("summary") ||
                                                         headerText.contains("details") ||
                                                         headerText.equals("full_description"))) {
                        descriptionColIndex = colIndex;
                        System.out.println("DEBUG - Found Description column at index " + colIndex + ": '" + headerText + "'");
                    }
                }
            }
            
            // If we couldn't find specific headers, use fallback logic
            if (descriptionColIndex == -1) {
                // Try to find the longest text column as description
                descriptionColIndex = findLongestTextColumn(sheet, headerRow);
                System.out.println("DEBUG - Using fallback: Description column at index " + descriptionColIndex);
            }
            
            if (companyColIndex == -1) {
                // Use first column as company if not found
                companyColIndex = 0;
                System.out.println("DEBUG - Using fallback: Company column at index 0");
            }
            
            if (roleColIndex == -1) {
                // Use second column as role if not found, or next available column
                roleColIndex = (companyColIndex == 0) ? 1 : 0;
                if (roleColIndex == descriptionColIndex) {
                    roleColIndex = (roleColIndex + 1) % headerRow.getLastCellNum();
                }
                System.out.println("DEBUG - Using fallback: Role column at index " + roleColIndex);
            }
            
            // Ensure we have unique column indices
            if (companyColIndex == roleColIndex || companyColIndex == descriptionColIndex || roleColIndex == descriptionColIndex) {
                // Find next available column for duplicates
                int nextAvailable = 0;
                while (nextAvailable == companyColIndex || nextAvailable == roleColIndex || nextAvailable == descriptionColIndex) {
                    nextAvailable++;
                }
                if (companyColIndex == roleColIndex) {
                    roleColIndex = nextAvailable;
                } else if (companyColIndex == descriptionColIndex) {
                    descriptionColIndex = nextAvailable;
                } else if (roleColIndex == descriptionColIndex) {
                    descriptionColIndex = nextAvailable;
                }
            }
            
            System.out.println("DEBUG - Final column mapping:");
            System.out.println("  Company: Column " + (companyColIndex + 1) + " (index " + companyColIndex + ")");
            System.out.println("  Role: Column " + (roleColIndex + 1) + " (index " + roleColIndex + ")");
            System.out.println("  Description: Column " + (descriptionColIndex + 1) + " (index " + descriptionColIndex + ")");
            
            int rowCount = 0;
            for (Row row : sheet) {
                rowCount++;
                // Skip header row
                if (row.getRowNum() == 0) {
                    continue;
                }
                
                // Get values from detected columns
                Cell companyCell = row.getCell(companyColIndex);
                Cell roleCell = row.getCell(roleColIndex);
                Cell descCell = row.getCell(descriptionColIndex);
                
                String companyName = (companyCell != null) ? getCellValueAsString(companyCell) : "";
                String roleName = (roleCell != null) ? getCellValueAsString(roleCell) : "";
                String description = (descCell != null) ? getCellValueAsString(descCell) : "";
                
                // Handle null values from getCellValueAsString
                companyName = (companyName != null) ? companyName : "";
                roleName = (roleName != null) ? roleName : "";
                description = (description != null) ? description : "";
                
                if (description != null && !description.trim().isEmpty()) {
                    // Extract apply link
                    String applyLink = "";
                    if (applyLinkColIndex != -1) {
                        Cell applyLinkCell = row.getCell(applyLinkColIndex);
                        if (applyLinkCell != null) {
                            applyLink = getCellValueAsString(applyLinkCell);
                            applyLink = (applyLink != null) ? applyLink.trim() : "";
                        }
                    }
                    
                    Map<String, String> jobInfo = new HashMap<>();
                    jobInfo.put("companyName", companyName != null ? companyName.trim() : "Unknown Company");
                    jobInfo.put("roleName", roleName != null ? roleName.trim() : "Unknown Role");
                    jobInfo.put("applyLink", applyLink);
                    jobInfo.put("description", description.trim());
                    jobDescriptions.add(jobInfo);
                    
                    // Debug logging for Excel parsing
                    System.out.println("DEBUG - Parsed Excel Row " + (row.getRowNum() + 1) + ": Company='" + companyName + "', Role='" + roleName + "', ApplyLink='" + applyLink + "', Description length=" + description.length());
                } else {
                    System.out.println("DEBUG - Skipping row " + (row.getRowNum() + 1) + " - no description found in detected description column");
                }
            }
            
            workbook.close();
            
            // Provide detailed error message if no job descriptions found
            if (jobDescriptions.isEmpty()) {
                throw new RuntimeException("No job descriptions found in the Excel file. Please ensure:\n" +
                    "1. Your Excel file has data in the correct format\n" +
                    "2. One column contains job descriptions (with header like 'Description', 'Requirements', etc.)\n" +
                    "3. You have at least one row of data (excluding header)\n" +
                    "4. The job description column is not empty\n\n" +
                    "Current file has " + rowCount + " rows, but no valid job descriptions were found.\n" +
                    "Detected columns: Company=" + (companyColIndex + 1) + ", Role=" + (roleColIndex + 1) + ", Description=" + (descriptionColIndex + 1));
            }
            
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw e; // Re-throw our custom error messages
            }
            throw new RuntimeException("Error parsing Excel file: " + e.getMessage() + "\nPlease ensure the file is a valid Excel file (.xlsx or .xls) with the correct format.", e);
        }
        
        return jobDescriptions;
    }
    
    private int findLongestTextColumn(Sheet sheet, Row headerRow) {
        int maxLength = 0;
        int longestColIndex = 0;
        
        // Check first few data rows to find the column with longest text
        for (int rowIndex = 1; rowIndex < Math.min(5, sheet.getPhysicalNumberOfRows()); rowIndex++) {
            Row dataRow = sheet.getRow(rowIndex);
            if (dataRow != null) {
                for (int colIndex = 0; colIndex < headerRow.getLastCellNum(); colIndex++) {
                    Cell cell = dataRow.getCell(colIndex);
                    if (cell != null) {
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && cellValue.length() > maxLength) {
                            maxLength = cellValue.length();
                            longestColIndex = colIndex;
                        }
                    }
                }
            }
        }
        
        return longestColIndex;
    }
    
    private void validateExcelStructure(Sheet sheet) {
        // Check if sheet has at least 1 column (minimum requirement)
        Row firstRow = sheet.getRow(0);
        if (firstRow == null) {
            throw new RuntimeException("Excel file appears to be empty. Please ensure it contains data.");
        }
        
        int columnCount = firstRow.getLastCellNum();
        if (columnCount < 1) {
            throw new RuntimeException("Excel file must have at least 1 column. Current file has " + columnCount + " columns.\n" +
                "The system will automatically detect which columns contain:\n" +
                "- Company information\n" +
                "- Job role/title\n" +
                "- Job description");
        }
        
        // Check if there are any rows with data
        int totalRows = sheet.getPhysicalNumberOfRows();
        if (totalRows <= 1) {
            throw new RuntimeException("Excel file must contain at least one row of data (excluding header). Current file has " + totalRows + " rows.");
        }
        
        System.out.println("DEBUG - Excel structure validation passed:");
        System.out.println("  Total columns: " + columnCount);
        System.out.println("  Total rows: " + totalRows);
        System.out.println("  Headers: " + getHeaderNames(firstRow));
    }
    
    private String getHeaderNames(Row headerRow) {
        StringBuilder headers = new StringBuilder();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerText = getCellValueAsString(cell);
                if (headerText != null && !headerText.trim().isEmpty()) {
                    if (headers.length() > 0) headers.append(", ");
                    headers.append("'").append(headerText.trim()).append("'");
                }
            }
        }
        return headers.toString();
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    private Map<String, Object> findBestMatch(List<Map<String, Object>> resumeResults) {
        Map<String, Object> bestMatch = null;
        double highestScore = -1;
        
        for (Map<String, Object> result : resumeResults) {
            if (!result.containsKey("error")) {
                Double score = ((List<Double>) result.get("atsScore")).get(0);
                if (score > highestScore) {
                    highestScore = score;
                    // Create a clean copy without circular references
                    bestMatch = new HashMap<>(result);
                    bestMatch.remove("allMatches"); // Remove circular reference
                }
            }
        }
        
        if (bestMatch == null) {
            // If no valid results, return the first one
            bestMatch = new HashMap<>(resumeResults.get(0));
            bestMatch.remove("allMatches"); // Remove circular reference
        }
        
        return bestMatch;
    }
    
    private Map<String, Object> calculateOverallSummary(List<Map<String, Object>> allResults, int totalJDs) {
        double totalScore = 0;
        double highestScore = 0;
        double lowestScore = 10;
        int validResults = 0;
        
        for (Map<String, Object> result : allResults) {
            if (!result.containsKey("error")) {
                Double score = ((List<Double>) result.get("atsScore")).get(0);
                totalScore += score;
                highestScore = Math.max(highestScore, score);
                lowestScore = Math.min(lowestScore, score);
                validResults++;
            }
        }
        
        double averageScore = validResults > 0 ? totalScore / validResults : 0;
        
        // Build summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalResumes", allResults.size());
        summary.put("totalJobDescriptions", totalJDs);
        summary.put("validResults", validResults);
        summary.put("averageScore", Math.round(averageScore * 100.0) / 100.0);
        summary.put("highestScore", Math.round(highestScore * 100.0) / 100.0);
        summary.put("lowestScore", Math.round(lowestScore * 100.0) / 100.0);
        
        // Find best overall match without circular references
        Map<String, Object> bestOverall = findBestOverallMatch(allResults);
        if (bestOverall != null) {
            summary.put("bestOverallMatch", bestOverall);
        }
        
        return summary;
    }
    
    private Map<String, Object> findBestOverallMatch(List<Map<String, Object>> allResults) {
        Map<String, Object> bestOverall = null;
        double highestScore = -1;
        
        for (Map<String, Object> result : allResults) {
            if (!result.containsKey("error")) {
                Double score = ((List<Double>) result.get("atsScore")).get(0);
                if (score > highestScore) {
                    highestScore = score;
                    // Create a clean copy without circular references
                    bestOverall = new HashMap<>(result);
                    bestOverall.remove("allMatches"); // Remove circular reference
                }
            }
        }
        
        return bestOverall;
    }
    
    private byte[] generateExcelWithResults(List<Map<String, String>> jobDescriptions, List<Map<String, Object>> allResults, MultipartFile[] resumes) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("JD Analysis Results");
            
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            
            CellStyle scoreStyle = workbook.createCellStyle();
            scoreStyle.setBorderBottom(BorderStyle.THIN);
            scoreStyle.setBorderTop(BorderStyle.THIN);
            scoreStyle.setBorderRight(BorderStyle.THIN);
            scoreStyle.setBorderLeft(BorderStyle.THIN);
            
            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Company Name", "Job Role", "Apply Link", "Job Description Summary", "Improvement Suggestions", "Best Match Resume", "ATS Score", "ATS Result", "New Resume Name", "All Resume Scores"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Set minimum width to prevent truncation
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }
            
            // Fill data
            int rowNum = 1;
            for (int jdIndex = 0; jdIndex < jobDescriptions.size(); jdIndex++) {
                Map<String, String> jobInfo = jobDescriptions.get(jdIndex);
                String jdText = jobInfo.get("description");
                String companyName = jobInfo.get("companyName");
                String roleName = jobInfo.get("roleName");
                
                // Debug logging for Excel data
                System.out.println("DEBUG - Excel Row " + (jdIndex + 1) + ": Company='" + companyName + "', Role='" + roleName + "'");
                
                // Find best resume for this JD
                Map<String, Object> bestResume = findBestResumeForJD(allResults, jdIndex);
                
                // Debug logging for best resume found
                if (bestResume != null) {
                    System.out.println("DEBUG - Best resume for JD " + (jdIndex + 1) + ": " + 
                        bestResume.get("resumeName") + ", Score: " + bestResume.get("atsScore") + 
                        ", New Name: " + bestResume.get("newResumeName"));
                } else {
                    System.out.println("DEBUG - No best resume found for JD " + (jdIndex + 1));
                }
                
                Row row = sheet.createRow(rowNum++);
                
                // Company Name
                Cell companyNameCell = row.createCell(0);
                companyNameCell.setCellValue(companyName != null ? companyName : "N/A");
                companyNameCell.setCellStyle(scoreStyle);
                
                // Job Role
                Cell jobRoleCell = row.createCell(1);
                jobRoleCell.setCellValue(roleName != null ? roleName : "N/A");
                jobRoleCell.setCellStyle(scoreStyle);
                
                // Apply Link
                Cell applyLinkCell = row.createCell(2);
                String applyLink = jobInfo.get("applyLink");
                applyLinkCell.setCellValue(applyLink != null ? applyLink : "N/A");
                applyLinkCell.setCellStyle(scoreStyle);
                
                // Job Description Summary
                Cell jdSummaryCell = row.createCell(3);
                String jdSummary = generateJobDescriptionSummary(jdText);
                jdSummaryCell.setCellValue(jdSummary);
                jdSummaryCell.setCellStyle(scoreStyle);
                
                // Improvement Suggestions
                Cell improvementCell = row.createCell(4);
                String improvementSuggestions = generateImprovementSuggestions(jdText, bestResume);
                improvementCell.setCellValue(improvementSuggestions);
                improvementCell.setCellStyle(scoreStyle);
                
                // Best Match Resume
                Cell bestMatchCell = row.createCell(5);
                if (bestResume != null && bestResume.get("resumeName") != null) {
                    bestMatchCell.setCellValue(bestResume.get("resumeName").toString());
                    System.out.println("DEBUG - Excel: Setting resume name for JD " + (jdIndex + 1) + ": " + bestResume.get("resumeName"));
                } else {
                    bestMatchCell.setCellValue("No match found");
                    System.out.println("DEBUG - Excel: No resume name found for JD " + (jdIndex + 1));
                }
                bestMatchCell.setCellStyle(scoreStyle);
                
                // ATS Score
                Cell scoreCell = row.createCell(6);
                if (bestResume != null && bestResume.get("atsScore") != null) {
                    try {
                        Double score = ((List<Double>) bestResume.get("atsScore")).get(0);
                        scoreCell.setCellValue(score);
                    } catch (Exception e) {
                        scoreCell.setCellValue(0.0);
                    }
                } else {
                    scoreCell.setCellValue(0.0);
                }
                scoreCell.setCellStyle(scoreStyle);
                
                // ATS Result (detailed analysis)
                Cell atsResultCell = row.createCell(7);
                if (bestResume != null) {
                    String atsResult = generateATSResultSummary(bestResume);
                    atsResultCell.setCellValue(atsResult);
                } else {
                    atsResultCell.setCellValue("No analysis available");
                }
                atsResultCell.setCellStyle(scoreStyle);
                
                // New Resume Name
                Cell newResumeNameCell = row.createCell(8);
                if (bestResume != null && bestResume.get("newResumeName") != null) {
                    newResumeNameCell.setCellValue(bestResume.get("newResumeName").toString());
                    System.out.println("DEBUG - Excel: Setting newResumeName for JD " + (jdIndex + 1) + ": " + bestResume.get("newResumeName"));
                } else {
                    newResumeNameCell.setCellValue("N/A");
                    System.out.println("DEBUG - Excel: No newResumeName found for JD " + (jdIndex + 1));
                }
                newResumeNameCell.setCellStyle(scoreStyle);
                
                // All Resume Scores
                Cell allScoresCell = row.createCell(9);
                String allScores = generateAllResumeScoresForJD(allResults, jdIndex);
                allScoresCell.setCellValue(allScores);
                allScoresCell.setCellStyle(scoreStyle);
            }
            
            // Convert to byte array
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }
    
    private Map<String, Object> findBestResumeForJD(List<Map<String, Object>> allResults, int jdIndex) {
        List<Map<String, Object>> candidatesForJD = new ArrayList<>();
        
        // Collect all resumes for this specific JD
        for (Map<String, Object> resumeResult : allResults) {
            if (!resumeResult.containsKey("error")) {
                // Check if this resume has a match for the specific JD
                List<Map<String, Object>> allMatches = (List<Map<String, Object>>) resumeResult.get("allMatches");
                if (allMatches != null) {
                    for (Map<String, Object> match : allMatches) {
                        if (match.get("jdIndex") != null && match.get("jdIndex").equals(jdIndex)) {
                                // Create a clean copy without circular references
                            Map<String, Object> cleanMatch = new HashMap<>(match);
                            cleanMatch.remove("allMatches"); // Remove circular reference
                                
                                // Copy the newResumeName from the main resume result if available
                                if (resumeResult.containsKey("newResumeName")) {
                                cleanMatch.put("newResumeName", resumeResult.get("newResumeName"));
                                }
                                
                            candidatesForJD.add(cleanMatch);
                            }
                        }
                    }
                }
            }
        
        // Sort by score (highest first) - this handles tie-breaking by selecting first
        candidatesForJD.sort((a, b) -> {
            Double scoreA = ((List<Double>) a.get("atsScore")).get(0);
            Double scoreB = ((List<Double>) b.get("atsScore")).get(0);
            return scoreB.compareTo(scoreA); // Descending order
        });
        
        // Return the first one (highest score, or first if tied)
        Map<String, Object> bestResume = null;
        if (!candidatesForJD.isEmpty()) {
            bestResume = candidatesForJD.get(0);
            Double score = ((List<Double>) bestResume.get("atsScore")).get(0);
            
            // Debug logging
            System.out.println("DEBUG - Found best resume for JD " + jdIndex + ": " + 
                bestResume.get("resumeName") + " with score " + score + 
                ", newResumeName: " + bestResume.get("newResumeName"));
        }
        
        if (bestResume == null) {
            System.out.println("DEBUG - No best resume found for JD " + jdIndex);
        }
        
        return bestResume;
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Download a renamed resume file
     */
    public byte[] downloadRenamedResume(MultipartFile originalResume, String newResumeName) throws IOException {
        try {
            // Get the original file extension
            String originalFilename = originalResume.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // Create the new filename with extension
            String finalFilename = newResumeName + fileExtension;
            
            // Read the original file content
            byte[] fileContent = originalResume.getBytes();
            
            // For now, we'll return the original file content
            // In a production system, you might want to store the renamed file
            System.out.println("DEBUG - Downloading renamed resume: " + finalFilename + " (original: " + originalFilename + ")");
            
            return fileContent;
        } catch (Exception e) {
            System.out.println("DEBUG - Error downloading renamed resume: " + e.getMessage());
            throw new RuntimeException("Failed to download renamed resume: " + e.getMessage(), e);
        }
    }

    private String generateCacheKey(String mode, String content) {
        try {
            // Create a more unique cache key by including content length and hash
            String contentHash = String.valueOf(content.hashCode());
            String contentLength = String.valueOf(content.length());
            String key = mode + ":" + contentLength + ":" + contentHash;
            
            // Use MD5 for final hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(key.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            System.out.println("DEBUG - Generated cache key: " + hexString.toString().substring(0, 8) + "... for content length: " + contentLength);
            return hexString.toString();
        } catch (Exception e) {
            // Fallback to simple hash
            String fallbackKey = mode + ":" + content.hashCode() + ":" + content.length();
            System.out.println("DEBUG - Using fallback cache key: " + fallbackKey.hashCode());
            return String.valueOf(fallbackKey.hashCode());
        }
    }

    private Map<String, Object> getCachedResult(String cacheKey) {
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            System.out.println("DEBUG - Cache HIT for key: " + cacheKey.substring(0, 8) + "...");
            return responseCache.get(cacheKey);
        }
        System.out.println("DEBUG - Cache MISS for key: " + cacheKey.substring(0, 8) + "...");
        return null;
    }

    private void cacheResult(String cacheKey, Map<String, Object> result) {
        responseCache.put(cacheKey, result);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        System.out.println("DEBUG - Cached result for key: " + cacheKey.substring(0, 8) + "...");
        
        // Clean up old cache entries (keep only last 50 entries to reduce memory usage)
        if (responseCache.size() > 50) {
            cleanupOldCacheEntries();
        }
    }

    private void cleanupOldCacheEntries() {
        long currentTime = System.currentTimeMillis();
        List<String> keysToRemove = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : cacheTimestamps.entrySet()) {
            if (currentTime - entry.getValue() > CACHE_DURATION) {
                keysToRemove.add(entry.getKey());
            }
        }
        
        for (String key : keysToRemove) {
            responseCache.remove(key);
            cacheTimestamps.remove(key);
        }
    }

    // Method to clear all cache (for debugging)
    public void clearCache() {
        responseCache.clear();
        cacheTimestamps.clear();
        System.out.println("DEBUG - Cache cleared manually");
    }

    // Method to get cache status (for debugging)
    public Map<String, Object> getCacheStatus() {
        return Map.of(
            "cacheEnabled", cacheEnabled,
            "cacheSize", responseCache.size(),
            "cacheDuration", CACHE_DURATION / (1000 * 60 * 60) + " hours",
            "timestampCount", cacheTimestamps.size()
        );
    }
    
    // Method to get API key dynamically
    private String getApiKey() {
        // First try to read from file
        try {
            if (apiKeyReader != null) {
                String fileKey = apiKeyReader.readApiKey();
                if (fileKey != null) {
                    return fileKey;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not read API key from file: {}", e.getMessage());
        }
        
        // Fallback to environment variable or configured value
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        
        return apiKey;
    }
    
    public String getApiKeyForHealthCheck() {
        return getApiKey();
    }

    private Map<String, Object> parseMode1Response(String response) {
        Map<String, Object> result = new HashMap<>();
        
        // Debug: Print the full AI response
        System.out.println("DEBUG - Full AI Response:");
        System.out.println(response);
        System.out.println("DEBUG - End of AI Response");
        
        try {
            // Extract career summary
            Pattern careerPattern = Pattern.compile("(?i)1\\.\\s*Career Summary\\s*:?\\s*(.*?)(?=\\s*2\\.|$)", Pattern.DOTALL);
            Matcher careerMatcher = careerPattern.matcher(response);
            if (careerMatcher.find()) {
                String careerText = careerMatcher.group(1).trim();
                if (!careerText.isEmpty() && !careerText.equalsIgnoreCase("N/A")) {
                    result.put("careerSummary", List.of(careerText));
                } else {
                    result.put("careerSummary", List.of("Career summary not provided"));
                }
            } else {
                result.put("careerSummary", List.of("Career summary not found"));
            }

            // Extract ATS score - look for "Score: X" format first
            Pattern scorePattern = Pattern.compile("(?i)Score:\\s*(\\d+(?:\\.\\d+)?)");
            Matcher scoreMatcher = scorePattern.matcher(response);
            if (scoreMatcher.find()) {
                result.put("atsScore", List.of(Double.parseDouble(scoreMatcher.group(1))));
            } else {
                // Fallback to old pattern
                Pattern oldScorePattern = Pattern.compile("(?i)2\\.\\s*ATS Score\\s*:?\\s*(\\d+(?:\\.\\d+)?)");
                Matcher oldScoreMatcher = oldScorePattern.matcher(response);
                if (oldScoreMatcher.find()) {
                    result.put("atsScore", List.of(Double.parseDouble(oldScoreMatcher.group(1))));
                } else {
                    // Default score if not found
                    result.put("atsScore", List.of(5.0));
                }
            }

            // Extract strengths and weaknesses - new format
            Pattern swPattern = Pattern.compile("(?i)3\\.\\s*Strengths and Weaknesses\\s*:?\\s*(.*?)(?=\\s*4\\.|$)", Pattern.DOTALL);
            Matcher swMatcher = swPattern.matcher(response);
            List<String> strengths = new ArrayList<>();
            List<String> weaknesses = new ArrayList<>();
            if (swMatcher.find()) {
                String swText = swMatcher.group(1).trim();
                
                // Look for "Strengths: [content]" format
                Pattern strengthsPattern = Pattern.compile("(?i)Strengths:\\s*\\[(.*?)\\]", Pattern.DOTALL);
                Matcher strengthsMatcher = strengthsPattern.matcher(swText);
                if (strengthsMatcher.find()) {
                    String strengthsContent = strengthsMatcher.group(1).trim();
                    if (!strengthsContent.isEmpty() && !strengthsContent.equalsIgnoreCase("None")) {
                        strengths.add(strengthsContent);
                    }
                }
                
                // Look for "Weaknesses: [content]" format
                Pattern weaknessesPattern = Pattern.compile("(?i)Weaknesses:\\s*\\[(.*?)\\]", Pattern.DOTALL);
                Matcher weaknessesMatcher = weaknessesPattern.matcher(swText);
                if (weaknessesMatcher.find()) {
                    String weaknessesContent = weaknessesMatcher.group(1).trim();
                    if (!weaknessesContent.isEmpty() && !weaknessesContent.equalsIgnoreCase("None")) {
                        weaknesses.add(weaknessesContent);
                    }
                }
                
                // Fallback to old parsing if new format not found
                if (strengths.isEmpty() && weaknesses.isEmpty()) {
                    Pattern splitPattern = Pattern.compile("(?i)(Strengths\\s*[:\\-\\u2014]?)(.*?)(?=(Weaknesses\\s*[:\\-\\u2014]?|$))(?:(Weaknesses\\s*[:\\-\\u2014]?)(.*))?", Pattern.DOTALL);
                    Matcher splitMatcher = splitPattern.matcher(swText);
                    if (splitMatcher.find()) {
                        String strengthsText = splitMatcher.group(2) != null ? splitMatcher.group(2).trim() : "";
                        String weaknessesText = splitMatcher.group(5) != null ? splitMatcher.group(5).trim() : "";
                        if (!strengthsText.isEmpty()) strengths.addAll(extractListItems(strengthsText));
                        if (!weaknessesText.isEmpty()) weaknesses.addAll(extractListItems(weaknessesText));
                    }
                }
            }
            if (strengths.isEmpty()) {
                strengths.add("No specific strengths identified");
            }
            if (weaknesses.isEmpty()) {
                weaknesses.add("No specific weaknesses identified");
            }
            result.put("strengths", strengths);
            result.put("weaknesses", weaknesses);

            // Extract suggestions
            Pattern suggestionsPattern = Pattern.compile("(?i)4\\.\\s*Suggestions to improve\\s*:?\\s*(.*?)(?=\\s*A\\.|$)", Pattern.DOTALL);
            Matcher suggestionsMatcher = suggestionsPattern.matcher(response);
            if (suggestionsMatcher.find()) {
                String suggestionsText = suggestionsMatcher.group(1);
                List<String> suggestions = extractListItems(suggestionsText, "suggestion", "improve");
                if (suggestions.isEmpty()) {
                    suggestions.add("Consider adding more details to your resume");
                }
                result.put("suggestions", suggestions);
            } else {
                result.put("suggestions", List.of("Consider adding more details to your resume"));
            }

            // Extract sections A, B, C, D with intelligent fallback
            // A. Work Experience
            result.put("workExperience", Map.of("matchedSkills", extractSectionContent(response, "A\\.\\s*Work Experience", "Work Experience")));
            
            // B. Certificates
            result.put("certificates", Map.of("matchedSkills", extractSectionContent(response, "B\\.\\s*Certificates", "Certificates")));
            
            // C. Projects
            result.put("projects", Map.of("matchedSkills", extractSectionContent(response, "C\\.\\s*Projects", "Projects")));
            
            // D. Technical Skills
            result.put("technicalSkills", Map.of("matchedSkills", extractSectionContent(response, "D\\.\\s*Technical Skills", "Technical Skills")));

        } catch (Exception e) {
            System.out.println("DEBUG - Exception in parsing: " + e.getMessage());
            e.printStackTrace();
            result.put("careerSummary", List.of("Error parsing response"));
            result.put("atsScore", List.of(5.0));
            result.put("strengths", List.of("Error parsing strengths"));
            result.put("weaknesses", List.of("Error parsing weaknesses"));
            result.put("suggestions", List.of("Error parsing suggestions"));
        }
        
        // Ensure all keys are present in the result for Mode 1
        if (!result.containsKey("projects")) {
            result.put("projects", Map.of("matchedSkills", List.of("None")));
        }
        if (!result.containsKey("certificates")) {
            result.put("certificates", Map.of("matchedSkills", List.of("None")));
        }
        if (!result.containsKey("technicalSkills")) {
            result.put("technicalSkills", Map.of("matchedSkills", List.of("None")));
        }
        if (!result.containsKey("workExperience")) {
            result.put("workExperience", Map.of("matchedSkills", List.of("None")));
        }
        if (!result.containsKey("strengths")) {
            result.put("strengths", new ArrayList<>());
        }
        if (!result.containsKey("weaknesses")) {
            result.put("weaknesses", new ArrayList<>());
        }
        if (!result.containsKey("careerSummary")) {
            result.put("careerSummary", new ArrayList<>());
        }
        if (!result.containsKey("suggestions")) {
            result.put("suggestions", new ArrayList<>());
        }
        
        return result;
    }
    
    private List<String> extractSectionContent(String response, String sectionPattern, String sectionName) {
        try {
            // Match everything after 'Matched Skills:' up to the next section or end
            Pattern pattern = Pattern.compile("(?i)" + sectionPattern + "\\s*\\nMatched Skills:\\s*(.*?)(?=\\n[A-D]\\. |\\n$)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                String content = matcher.group(1).trim();
                System.out.println("DEBUG - Extracted content for " + sectionName + ": '" + content + "'");
                
                if (!content.isEmpty() && !content.equalsIgnoreCase("None")) {
                    List<String> items = new ArrayList<>();
                    
                    // Split by newlines first
                    String[] lines = content.split("\\r?\\n");
                    for (String line : lines) {
                        line = line.replaceAll("^[\\-•*\\s]+", "").trim();
                        if (!line.isEmpty()) {
                            // If line contains commas, split by comma
                            if (line.contains(",")) {
                                String[] commaItems = line.split(",");
                                for (String item : commaItems) {
                                    String trimmedItem = item.trim();
                                    if (!trimmedItem.isEmpty()) {
                                        items.add(trimmedItem);
                                    }
                                }
                            } else {
                                items.add(line);
                            }
                        }
                    }
                    
                    if (!items.isEmpty()) {
                        System.out.println("DEBUG - Final items for " + sectionName + ": " + items);
                        return items;
                    }
                    // fallback: single line
                    return List.of(content);
                }
            }
            
            // Try fallback pattern for single line format
            Pattern fallbackPattern = Pattern.compile("(?i)" + sectionName + ".*?Matched Skills:\\s*([^\\n]+)", Pattern.DOTALL);
            Matcher fallbackMatcher = fallbackPattern.matcher(response);
            if (fallbackMatcher.find()) {
                String content = fallbackMatcher.group(1).trim();
                System.out.println("DEBUG - Fallback extracted content for " + sectionName + ": '" + content + "'");
                
                if (!content.isEmpty() && !content.equalsIgnoreCase("None")) {
                    // Split by comma if present
                    if (content.contains(",")) {
                        List<String> items = new ArrayList<>();
                        String[] commaItems = content.split(",");
                        for (String item : commaItems) {
                            String trimmedItem = item.trim();
                            if (!trimmedItem.isEmpty()) {
                                items.add(trimmedItem);
                            }
                        }
                        if (!items.isEmpty()) {
                            System.out.println("DEBUG - Fallback items for " + sectionName + ": " + items);
                            return items;
                        }
                    } else {
                        return List.of(content);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG - Error extracting content for " + sectionName + ": " + e.getMessage());
        }
        
        System.out.println("DEBUG - No content found for " + sectionName + ", returning None");
        return List.of("None");
    }

    private Map<String, Object> parseMode2Response(String response, String resumeContent) {
        Map<String, Object> result = new HashMap<>();
        
        // Debug: Print the first 1000 characters of the response to see the format
        System.out.println("DEBUG - AI Response (first 1000 chars): " + response.substring(0, Math.min(1000, response.length())));
        System.out.println("DEBUG - Full AI Response length: " + response.length());
        System.out.println("DEBUG - Looking for score patterns in response...");
        
        try {
            // Extract career summary
            Pattern careerPattern = Pattern.compile("(?i)1\\.\\s*Career Summary\\s*:?\\s*(.*?)(?=\\s*2\\.|$)", Pattern.DOTALL);
            Matcher careerMatcher = careerPattern.matcher(response);
            if (careerMatcher.find()) {
                result.put("careerSummary", List.of(careerMatcher.group(1).trim()));
            } else {
                result.put("careerSummary", List.of("N/A"));
            }

            // Extract ATS score - look for "Score: X" format first
            Pattern scorePattern = Pattern.compile("(?i)Score:\\s*(\\d+(?:\\.\\d+)?)");
            Matcher scoreMatcher = scorePattern.matcher(response);
            if (scoreMatcher.find()) {
                result.put("atsScore", List.of(Double.parseDouble(scoreMatcher.group(1))));
            } else {
                // Fallback to old patterns
                Pattern[] atsScorePatterns = {
                    Pattern.compile("(?i)2\\.\\s*ATS Score\\s*:?\\s*(\\d+(?:\\.\\d+)?)"),
                    Pattern.compile("(?i)ATS Score\\s*:?\\s*(\\d+(?:\\.\\d+)?)"),
                    Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*out of\\s*10")
                };
                
                boolean atsScoreFound = false;
                for (int i = 0; i < atsScorePatterns.length; i++) {
                    Pattern pattern = atsScorePatterns[i];
                    Matcher atsScoreMatcher = pattern.matcher(response);
                    if (atsScoreMatcher.find()) {
                        String scoreText = atsScoreMatcher.group(1);
                        System.out.println("DEBUG - Found score with pattern " + (i+1) + ": " + scoreText);
                        result.put("atsScore", List.of(Double.parseDouble(scoreText)));
                        atsScoreFound = true;
                        break;
                    }
                }
                
                if (!atsScoreFound) {
                    System.out.println("DEBUG - No score found in response, setting to 0.0");
                    result.put("atsScore", List.of(0.0));
                }
            }

            // Extract job details (company, role, match status)
            System.out.println("DEBUG - Looking for company and role in response...");
            
            // Try multiple patterns for company
            String companyName = "Unknown Company";
            Pattern[] companyPatterns = {
                Pattern.compile("(?i)Company:\\s*\\[(.*?)\\]"),
                Pattern.compile("(?i)Company\\s*:\\s*(.*?)(?=\\s*Role:|\\s*Match Status:|$)", Pattern.DOTALL),
                Pattern.compile("(?i)Company\\s*[:\\-\\u2014]?\\s*(.*?)(?=\\s*Role|\\s*Match Status|$)", Pattern.DOTALL)
            };
            
            boolean companyFound = false;
            for (int i = 0; i < companyPatterns.length && !companyFound; i++) {
                Pattern pattern = companyPatterns[i];
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    companyName = matcher.group(1).trim();
                    System.out.println("DEBUG - Found company with pattern " + (i+1) + ": '" + companyName + "'");
                    if (!companyName.isEmpty() && !companyName.equalsIgnoreCase("None")) {
                        companyFound = true;
                    }
                }
            }
            
            if (!companyFound) {
                System.out.println("DEBUG - Company not found, using default: 'Unknown Company'");
                companyName = "Unknown Company";
            }
            result.put("companyName", companyName);

            // Try multiple patterns for role
            String roleName = "Unknown Role";
            Pattern[] rolePatterns = {
                Pattern.compile("(?i)Role:\\s*\\[(.*?)\\]"),
                Pattern.compile("(?i)Role\\s*:\\s*(.*?)(?=\\s*Match Status:|$)", Pattern.DOTALL),
                Pattern.compile("(?i)Role\\s*[:\\-\\u2014]?\\s*(.*?)(?=\\s*Match Status|$)", Pattern.DOTALL)
            };
            
            boolean roleFound = false;
            for (int i = 0; i < rolePatterns.length && !roleFound; i++) {
                Pattern pattern = rolePatterns[i];
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    roleName = matcher.group(1).trim();
                    System.out.println("DEBUG - Found role with pattern " + (i+1) + ": '" + roleName + "'");
                    if (!roleName.isEmpty() && !roleName.equalsIgnoreCase("None")) {
                        roleFound = true;
                    }
                }
            }
            
            if (!roleFound) {
                System.out.println("DEBUG - Role not found, using default: 'Unknown Role'");
                roleName = "Unknown Role";
            }
            result.put("roleName", roleName);

            Pattern matchStatusPattern = Pattern.compile("(?i)Match Status:\\s*\\[(.*?)\\]");
            Matcher matchStatusMatcher = matchStatusPattern.matcher(response);
            String matchStatus = "UNMATCHED";
            if (matchStatusMatcher.find()) {
                matchStatus = matchStatusMatcher.group(1).trim();
            }
            result.put("matchStatus", matchStatus);

            // Generate new resume name based on job details and resume content
            String newResumeName = generateNewResumeName(companyName, roleName, resumeContent);
            result.put("newResumeName", newResumeName);

            // Extract strengths and weaknesses - more flexible parsing
            Pattern swPattern = Pattern.compile("(?i)4\\.\\s*Strengths and Weaknesses\\s*:?\\s*(.*?)(?=\\s*5\\.|$)", Pattern.DOTALL);
            Matcher swMatcher = swPattern.matcher(response);
            List<String> strengths = new ArrayList<>();
            List<String> weaknesses = new ArrayList<>();
            if (swMatcher.find()) {
                String swText = swMatcher.group(1).trim();
                
                // Try multiple patterns for strengths
                Pattern[] strengthsPatterns = {
                    Pattern.compile("(?i)Strengths:\\s*\\[(.*?)\\]", Pattern.DOTALL),
                    Pattern.compile("(?i)Strengths:\\s*(.*?)(?=\\s*Weaknesses:|$)", Pattern.DOTALL),
                    Pattern.compile("(?i)Strengths\\s*[:\\-\\u2014]?\\s*(.*?)(?=\\s*Weaknesses|$)", Pattern.DOTALL)
                };
                
                for (Pattern pattern : strengthsPatterns) {
                    Matcher matcher = pattern.matcher(swText);
                    if (matcher.find()) {
                        String content = matcher.group(1).trim();
                        if (!content.isEmpty() && !content.equalsIgnoreCase("None")) {
                            strengths.addAll(extractListItems(content));
                            break;
                        }
                    }
                }
                
                // Try multiple patterns for weaknesses
                Pattern[] weaknessesPatterns = {
                    Pattern.compile("(?i)Weaknesses:\\s*\\[(.*?)\\]", Pattern.DOTALL),
                    Pattern.compile("(?i)Weaknesses:\\s*(.*?)(?=\\s*Suggestions:|$)", Pattern.DOTALL),
                    Pattern.compile("(?i)Weaknesses\\s*[:\\-\\u2014]?\\s*(.*?)(?=\\s*Suggestions|$)", Pattern.DOTALL)
                };
                
                for (Pattern pattern : weaknessesPatterns) {
                    Matcher matcher = pattern.matcher(swText);
                    if (matcher.find()) {
                        String content = matcher.group(1).trim();
                        if (!content.isEmpty() && !content.equalsIgnoreCase("None")) {
                            weaknesses.addAll(extractListItems(content));
                            break;
                        }
                    }
                }
            }
            
            if (strengths.isEmpty()) {
                strengths.add("N/A");
            }
            if (weaknesses.isEmpty()) {
                weaknesses.add("N/A");
            }
            result.put("strengths", strengths);
            result.put("weaknesses", weaknesses);

            // Extract suggestions
            Pattern suggestionsPattern = Pattern.compile("(?i)5\\.\\s*Suggestions to improve\\s*:?\\s*(.*?)(?=\\s*A\\.|$)", Pattern.DOTALL);
            Matcher suggestionsMatcher = suggestionsPattern.matcher(response);
            if (suggestionsMatcher.find()) {
                String suggestionsText = suggestionsMatcher.group(1);
                List<String> suggestions = extractListItems(suggestionsText, "suggestion", "improve");
                if (suggestions.isEmpty()) {
                    suggestions.add("Consider adding more details to your resume");
                }
                result.put("suggestions", suggestions);
            } else {
                result.put("suggestions", List.of("Consider adding more details to your resume"));
            }

            // Extract sections A, B, C, D with more flexible parsing
            // A. Work Experience
            Map<String, Object> workExp = extractSectionData(response, "A\\.\\s*Work Experience", "Work Experience");
            result.put("workExperience", workExp);
            
            // B. Certificates
            Map<String, Object> cert = extractSectionData(response, "B\\.\\s*Certificates", "Certificates");
            result.put("certificates", cert);
            
            // C. Projects
            Map<String, Object> proj = extractSectionData(response, "C\\.\\s*Projects", "Projects");
            result.put("projects", proj);
            
            // D. Technical Skills
            Map<String, Object> tech = extractSectionData(response, "D\\.\\s*Technical Skills", "Technical Skills");
            result.put("technicalSkills", tech);

        } catch (Exception e) {
            System.out.println("DEBUG - Exception in parsing: " + e.getMessage());
            e.printStackTrace();
            result.put("careerSummary", List.of("Error parsing response"));
            result.put("atsScore", List.of(5.0));
            result.put("strengths", List.of("Error parsing strengths"));
            result.put("weaknesses", List.of("Error parsing weaknesses"));
            result.put("suggestions", List.of("Error parsing suggestions"));
        }

        return result;
    }

    private Map<String, Object> extractSectionData(String response, String sectionPattern, String sectionName) {
        Map<String, Object> sectionData = new HashMap<>();
        List<String> matchedSkills = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        
        // Try multiple patterns for each section
        Pattern[] patterns = {
            // Pattern 1: Exact format with brackets
            Pattern.compile("(?i)" + sectionPattern + "\\s*\\nMatched Skills:\\s*\\[(.*?)\\]\\s*\\nGaps:\\s*\\[(.*?)\\]", Pattern.DOTALL),
            // Pattern 2: Without brackets
            Pattern.compile("(?i)" + sectionPattern + "\\s*\\nMatched Skills:\\s*(.*?)\\s*\\nGaps:\\s*(.*?)(?=\\s*[A-Z]\\.|$)", Pattern.DOTALL),
            // Pattern 3: More flexible format
            Pattern.compile("(?i)" + sectionPattern + ".*?Matched Skills:\\s*(.*?)\\s*Gaps:\\s*(.*?)(?=\\s*[A-Z]\\.|$)", Pattern.DOTALL),
            // Pattern 4: Fallback - just look for the section
            Pattern.compile("(?i)" + sectionPattern + ".*?(?=\\s*[A-Z]\\.|$)", Pattern.DOTALL)
        };
        
        boolean found = false;
        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = patterns[i];
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                if (i < 3) {
                    // Patterns 1-3 have groups for matched skills and gaps
                    String matchedText = matcher.group(1).trim();
                    String gapsText = matcher.group(2).trim();
                    
                    if (!matchedText.isEmpty() && !matchedText.equalsIgnoreCase("None")) {
                        matchedSkills.addAll(extractListItems(matchedText));
                    }
                    if (!gapsText.isEmpty() && !gapsText.equalsIgnoreCase("None")) {
                        gaps.addAll(extractListItems(gapsText));
                    }
                } else {
                    // Pattern 4 - extract from the entire section
                    String sectionText = matcher.group(0);
                    // Look for any mention of skills or gaps in the section
                    Pattern skillsPattern = Pattern.compile("(?i)Matched Skills:\\s*(.*?)(?=\\s*Gaps:|$)", Pattern.DOTALL);
                    Pattern gapsPattern = Pattern.compile("(?i)Gaps:\\s*(.*?)(?=\\s*[A-Z]\\.|$)", Pattern.DOTALL);
                    
                    Matcher skillsMatcher = skillsPattern.matcher(sectionText);
                    if (skillsMatcher.find()) {
                        String skillsText = skillsMatcher.group(1).trim();
                        if (!skillsText.isEmpty() && !skillsText.equalsIgnoreCase("None")) {
                            matchedSkills.addAll(extractListItems(skillsText));
                        }
                    }
                    
                    Matcher gapsMatcher = gapsPattern.matcher(sectionText);
                    if (gapsMatcher.find()) {
                        String gapsText = gapsMatcher.group(1).trim();
                        if (!gapsText.isEmpty() && !gapsText.equalsIgnoreCase("None")) {
                            gaps.addAll(extractListItems(gapsText));
                        }
                    }
                }
                found = true;
                break;
            }
        }
        
        if (!found) {
            // If no section found, try to extract any relevant information from the response
            Pattern fallbackPattern = Pattern.compile("(?i)" + sectionName + ".*?(?=\\s*[A-Z]\\.|$)", Pattern.DOTALL);
            Matcher fallbackMatcher = fallbackPattern.matcher(response);
            if (fallbackMatcher.find()) {
                String sectionText = fallbackMatcher.group(0);
                // Look for any skills or gaps mentioned
                Pattern skillsPattern = Pattern.compile("(?i)(skills?|experience|knowledge).*?(?=\\s*[A-Z]\\.|$)", Pattern.DOTALL);
                Matcher skillsMatcher = skillsPattern.matcher(sectionText);
                if (skillsMatcher.find()) {
                    matchedSkills.add("Some relevant experience found");
                }
            }
        }
        
        if (matchedSkills.isEmpty()) {
            matchedSkills.add("None");
        }
        if (gaps.isEmpty()) {
            gaps.add("None");
        }
        
        sectionData.put("matchedSkills", matchedSkills);
        sectionData.put("gaps", gaps);
        
        return sectionData;
    }

    private String generateNewResumeName(String companyName, String roleName, String resumeContent) {
        try {
            // Extract username from resume content (first line usually contains name)
            String username = extractUsernameFromResume(resumeContent);
            
            // Validate username - if it contains error messages, use fallback
            if (username == null || username.isEmpty() || 
                username.toLowerCase().contains("error") || 
                username.toLowerCase().contains("enable") ||
                username.toLowerCase().contains("you_need_to") ||
                username.equals("Resume")) {
                // Try to get a better fallback name
                username = "Candidate";
            }
            
            // Clean and format company name - preserve more characters for better readability
            String cleanCompany = companyName != null ? companyName.replaceAll("[^a-zA-Z0-9\\s\\-]", "").trim() : "";
            if (cleanCompany.isEmpty()) {
                cleanCompany = "UnknownCompany";
            }
            
            // Clean and format role name - preserve more characters for better readability
            String cleanRole = roleName != null ? roleName.replaceAll("[^a-zA-Z0-9\\s\\-]", "").trim() : "";
            if (cleanRole.isEmpty()) {
                cleanRole = "UnknownRole";
            }
            
            // Generate unique ID (timestamp-based)
            String timestamp = String.valueOf(System.currentTimeMillis());
            String shortTimestamp = timestamp.substring(timestamp.length() - 6); // Last 6 digits
            
            // Format: Username_CompanyName_RoleName_ID
            // Replace spaces with underscores for filename compatibility
            return String.format("%s_%s_%s_%s", 
                username.replaceAll("\\s+", "_"),
                cleanCompany.replaceAll("\\s+", "_"), 
                cleanRole.replaceAll("\\s+", "_"), 
                shortTimestamp);
                
        } catch (Exception e) {
            System.out.println("DEBUG - Error in generateNewResumeName: " + e.getMessage());
            // Return a safe fallback name
            String timestamp = String.valueOf(System.currentTimeMillis());
            String shortTimestamp = timestamp.substring(timestamp.length() - 6);
            return String.format("Resume_%s_%s", 
                (companyName != null ? companyName.replaceAll("[^a-zA-Z0-9]", "") : "Unknown"),
                shortTimestamp);
        }
    }
    
    private String extractUsernameFromResume(String resumeContent) {
        if (resumeContent == null || resumeContent.trim().isEmpty()) {
            return "Resume";
        }
        
        try {
            // Split content into lines and get the first few lines
            String[] lines = resumeContent.split("\\r?\\n");
            
            // Look through the first 10 lines for a name
            for (int i = 0; i < Math.min(10, lines.length); i++) {
                String line = lines[i].trim();
                if (!line.isEmpty()) {
                    // Skip lines that look like error messages or system messages
                    String lowerLine = line.toLowerCase();
                    if (lowerLine.contains("error") || 
                        lowerLine.contains("enable") || 
                        lowerLine.contains("you need to") ||
                        lowerLine.contains("please") ||
                        lowerLine.contains("click") ||
                        lowerLine.contains("javascript") ||
                        lowerLine.contains("resume") ||
                        lowerLine.contains("curriculum") ||
                        lowerLine.contains("vitae") ||
                        lowerLine.contains("cv") ||
                        lowerLine.length() > 100) {
                        continue;
                    }
                    
                    // Look for lines that contain typical name patterns
                    // Names usually have 2-4 words, each 2-20 characters, mostly letters
                    String[] words = line.split("\\s+");
                    if (words.length >= 2 && words.length <= 4) {
                        boolean looksLikeName = true;
                        StringBuilder nameBuilder = new StringBuilder();
                        
                        for (String word : words) {
                            // Clean each word (remove special characters, keep only letters)
                            String cleanWord = word.replaceAll("[^a-zA-Z]", "").trim();
                            
                            // Check if word looks like a name part (2-20 letters)
                            if (cleanWord.length() < 2 || cleanWord.length() > 20) {
                                looksLikeName = false;
                                break;
                            }
                            
                            // Check if word contains mostly letters (at least 80%)
                            long letterCount = cleanWord.chars().filter(Character::isLetter).count();
                            if (letterCount < cleanWord.length() * 0.8) {
                                looksLikeName = false;
                                break;
                            }
                            
                            if (nameBuilder.length() > 0) nameBuilder.append(" ");
                            nameBuilder.append(cleanWord);
                        }
                        
                        if (looksLikeName && nameBuilder.length() > 0) {
                            String extractedName = nameBuilder.toString().trim();
                            System.out.println("DEBUG - Extracted name: '" + extractedName + "' from line: '" + line + "'");
                            return extractedName;
                        }
                    }
                }
            }
            
            // If no name found in first 10 lines, try a simpler approach
            for (int i = 0; i < Math.min(5, lines.length); i++) {
                String line = lines[i].trim();
                if (!line.isEmpty() && line.length() < 50) {
                    String cleanLine = line.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
                    if (cleanLine.length() > 2 && cleanLine.length() < 50) {
                        System.out.println("DEBUG - Using fallback name: '" + cleanLine + "' from line: '" + line + "'");
                        return cleanLine;
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("DEBUG - Error extracting username: " + e.getMessage());
        }
        
        return "Resume";
    }
    
    /**
     * Generate a concise and accurate summary of the job description using OpenAI
     */
    private String generateJobDescriptionSummary(String jobDescription) {
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return "No job description provided";
        }

        String prompt = "Summarize the following job description in exactly 100 words, formatted as 4-5 lines. Focus on:\n" +
                       "1. Key responsibilities and duties\n" +
                       "2. Required skills and qualifications\n" +
                       "3. Experience level and seniority\n" +
                       "4. Industry or domain focus\n\n" +
                       "Requirements:\n" +
                       "- Use exactly 100 words (count carefully)\n" +
                       "- Format as 4-5 lines for better readability\n" +
                       "- Keep it professional and accurate\n" +
                       "- Do not include any introductory phrases\n" +
                       "- Each line should be a complete thought\n\n" +
                       "Job Description:\n" + jobDescription;

        try {
            String currentApiKey = getApiKey();
            if (currentApiKey == null || currentApiKey.equals("your-openai-api-key-here")) {
                throw new RuntimeException("OpenAI API key not configured");
            }
            String aiResponse = openAIUtils.callOpenAI(currentApiKey, prompt);
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                // Clean up the response
                String summary = aiResponse.trim();
                
                // Remove any unwanted introductory phrases
                summary = summary.replaceFirst("^(Here is a summary:|In summary:|Summary:|The job description|This position)", "").trim();
                
                // Ensure it starts with a capital letter
                if (!summary.isEmpty()) {
                    summary = Character.toUpperCase(summary.charAt(0)) + summary.substring(1);
                }
                
                // Ensure proper line breaks for 4-5 lines format
                summary = summary.replaceAll("\\n\\s*\\n", "\n"); // Remove extra blank lines
                summary = summary.replaceAll("\\n+", "\n"); // Replace multiple newlines with single
                
                // Count words and adjust if needed
                String[] words = summary.split("\\s+");
                if (words.length > 100) {
                    // Truncate to exactly 100 words
                    StringBuilder truncated = new StringBuilder();
                    for (int i = 0; i < 100; i++) {
                        if (i > 0) truncated.append(" ");
                        truncated.append(words[i]);
                    }
                    summary = truncated.toString();
                }
                
                // Ensure it ends with a period
                if (!summary.endsWith(".") && !summary.endsWith("!") && !summary.endsWith("?")) {
                    summary += ".";
                }
                
                return summary;
            }
            return "Summary not available";
        } catch (Exception e) {
            System.out.println("DEBUG - Error generating job description summary with OpenAI: " + e.getMessage());
            // Fallback to simple truncation if AI fails
            String fallback = jobDescription.trim();
            if (fallback.length() > 200) {
                fallback = fallback.substring(0, 197) + "...";
            }
            return fallback;
        }
    }
    
    /**
     * Generate improvement suggestions for higher match score using AI analysis
     */
    private String generateImprovementSuggestions(String jobDescription, Map<String, Object> bestResume) {
        try {
            if (bestResume == null || bestResume.containsKey("error")) {
                return "No resume available for analysis";
            }
            
            // Get current score
            Double currentScore = 0.0;
            try {
                currentScore = ((List<Double>) bestResume.get("atsScore")).get(0);
            } catch (Exception e) {
                currentScore = 0.0;
            }
            
            // Build resume analysis data for AI
            StringBuilder resumeAnalysis = new StringBuilder();
            resumeAnalysis.append("RESUME ANALYSIS:\n");
            resumeAnalysis.append("Current ATS Score: ").append(String.format("%.1f", currentScore)).append("/10\n\n");
            
            // Add strengths
            if (bestResume.containsKey("strengths")) {
                @SuppressWarnings("unchecked")
                List<String> strengths = (List<String>) bestResume.get("strengths");
                if (strengths != null && !strengths.isEmpty()) {
                    resumeAnalysis.append("Strengths: ").append(String.join(", ", strengths)).append("\n");
                }
            }
            
            // Add work experience
            if (bestResume.containsKey("workExperience")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> workExp = (Map<String, Object>) bestResume.get("workExperience");
                if (workExp != null && workExp.containsKey("matchedSkills")) {
                    @SuppressWarnings("unchecked")
                    List<String> matchedSkills = (List<String>) workExp.get("matchedSkills");
                    if (matchedSkills != null && !matchedSkills.isEmpty()) {
                        resumeAnalysis.append("Work Experience Skills: ").append(String.join(", ", matchedSkills)).append("\n");
                    }
                }
            }
            
            // Add technical skills
            if (bestResume.containsKey("technicalSkills")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> techSkills = (Map<String, Object>) bestResume.get("technicalSkills");
                if (techSkills != null && techSkills.containsKey("matchedSkills")) {
                    @SuppressWarnings("unchecked")
                    List<String> matchedTechSkills = (List<String>) techSkills.get("matchedSkills");
                    if (matchedTechSkills != null && !matchedTechSkills.isEmpty()) {
                        resumeAnalysis.append("Technical Skills: ").append(String.join(", ", matchedTechSkills)).append("\n");
                    }
                }
            }
            
            // Add gaps if available
            if (bestResume.containsKey("workExperience")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> workExp = (Map<String, Object>) bestResume.get("workExperience");
                if (workExp != null && workExp.containsKey("gaps")) {
                    @SuppressWarnings("unchecked")
                    List<String> gaps = (List<String>) workExp.get("gaps");
                    if (gaps != null && !gaps.isEmpty() && !gaps.get(0).equals("None")) {
                        resumeAnalysis.append("Identified Gaps: ").append(String.join(", ", gaps)).append("\n");
                    }
                }
            }
            
            String prompt = "Based on the following job description and resume analysis, provide specific, actionable improvement suggestions to increase the ATS match score. " +
                          "Focus on concrete steps the candidate can take to better align with the job requirements.\n\n" +
                          "JOB DESCRIPTION:\n" + jobDescription + "\n\n" +
                          resumeAnalysis.toString() + "\n" +
                          "Format your response as follows:\n" +
                          "TO IMPROVE MATCH SCORE (Current: " + String.format("%.1f", currentScore) + "/10):\n" +
                          "=====================================\n\n" +
                          "Then provide 5-7 specific, actionable suggestions in bullet point format. " +
                          "Be specific about what skills to add, experiences to highlight, or content to improve. " +
                          "Keep each suggestion concise but detailed enough to be actionable.";

            String currentApiKey = getApiKey();
            if (currentApiKey == null || currentApiKey.equals("your-openai-api-key-here")) {
                throw new RuntimeException("OpenAI API key not configured");
            }
            String aiResponse = openAIUtils.callOpenAI(currentApiKey, prompt);
            
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                // Clean up the response
                String suggestions = aiResponse.trim();
                
                // Remove any unwanted introductory phrases
                suggestions = suggestions.replaceFirst("^(Here are the suggestions:|Improvement suggestions:|Suggestions:)", "").trim();
                
                // Ensure it starts with the proper header
                if (!suggestions.startsWith("TO IMPROVE MATCH SCORE")) {
                    suggestions = "TO IMPROVE MATCH SCORE (Current: " + String.format("%.1f", currentScore) + "/10):\n" +
                                 "=====================================\n\n" + suggestions;
                }
                
                // Limit length for Excel cell
                if (suggestions.length() > 2000) {
                    suggestions = suggestions.substring(0, 1997) + "...";
                }
                
                return suggestions;
            }
            
            return "AI analysis not available";
            
        } catch (Exception e) {
            System.out.println("DEBUG - Error generating AI improvement suggestions: " + e.getMessage());
            return "Error generating suggestions";
        }
    }
    
    /**
     * Generate all resume scores for a specific job description
     */
    private String generateAllResumeScoresForJD(List<Map<String, Object>> allResults, int jdIndex) {
        StringBuilder scores = new StringBuilder();
        
        try {
            // Find all results for this specific JD
            List<Map<String, Object>> jdResults = new ArrayList<>();
            for (Map<String, Object> result : allResults) {
                if (result.containsKey("jdIndex") && 
                    result.get("jdIndex") != null && 
                    result.get("jdIndex").equals(jdIndex) &&
                    !result.containsKey("error")) {
                    jdResults.add(result);
                }
            }
            
            if (jdResults.isEmpty()) {
                return "No resumes analyzed for this JD";
            }
            
            // Sort by ATS score (highest first)
            jdResults.sort((a, b) -> {
                try {
                    Double scoreA = ((List<Double>) a.get("atsScore")).get(0);
                    Double scoreB = ((List<Double>) b.get("atsScore")).get(0);
                    return Double.compare(scoreB, scoreA); // Descending order
                } catch (Exception e) {
                    return 0;
                }
            });
            
            // Format scores line by line
            for (int i = 0; i < jdResults.size(); i++) {
                Map<String, Object> result = jdResults.get(i);
                String resumeName = result.get("resumeName") != null ? 
                    result.get("resumeName").toString() : "Unknown Resume";
                
                Double score = 0.0;
                try {
                    score = ((List<Double>) result.get("atsScore")).get(0);
                } catch (Exception e) {
                    score = 0.0;
                }
                
                String status = (i == 0) ? "SELECTED" : "NOT SELECTED";
                
                // Add line break between entries (except for the first one)
                if (i > 0) scores.append("\n");
                scores.append(String.format("%s: %.1f (%s)", resumeName, score, status));
            }
            
            return scores.toString();
            
        } catch (Exception e) {
            System.out.println("DEBUG - Error generating all resume scores: " + e.getMessage());
            return "Error generating scores";
        }
    }

    private List<String> extractListItems(String text, String... keywords) {
        List<String> items = new ArrayList<>();
        
        // Split by common list indicators
        String[] lines = text.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Remove bullet points, numbers, etc.
            line = line.replaceAll("^[•\\-\\*\\d\\.\\s]+", "").trim();
            if (!line.isEmpty()) {
                items.add(line);
            }
        }
        
        // If no items found, try to extract from the text
        if (items.isEmpty()) {
            // Look for sentences that might contain the keywords
            String[] sentences = text.split("[.!?]");
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (!sentence.isEmpty() && sentence.length() > 10) {
                    items.add(sentence);
                }
            }
        }
        
        return items;
    }
    
    /**
     * Generate a comprehensive ATS result explanation for Excel display using AI
     */
    private String generateATSResultSummary(Map<String, Object> bestResume) {
        try {
            if (bestResume == null || bestResume.containsKey("error")) {
                return "No resume analysis available";
            }
            
            // Get the ATS score
            Double score = 0.0;
            if (bestResume.get("atsScore") != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Double> scores = (List<Double>) bestResume.get("atsScore");
                    score = scores.get(0);
                } catch (Exception e) {
                    score = 0.0;
                }
            }
            
            // Build comprehensive resume analysis for AI
            StringBuilder resumeAnalysis = new StringBuilder();
            resumeAnalysis.append("RESUME ANALYSIS DATA:\n");
            resumeAnalysis.append("ATS Score: ").append(String.format("%.1f", score)).append("/10\n\n");
            
            // Add strengths
            if (bestResume.containsKey("strengths")) {
                @SuppressWarnings("unchecked")
                List<String> strengths = (List<String>) bestResume.get("strengths");
                if (strengths != null && !strengths.isEmpty()) {
                    resumeAnalysis.append("Strengths: ").append(String.join(", ", strengths)).append("\n");
                }
            }
            
            // Add work experience
            if (bestResume.containsKey("workExperience")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> workExp = (Map<String, Object>) bestResume.get("workExperience");
                if (workExp != null && workExp.containsKey("matchedSkills")) {
                    @SuppressWarnings("unchecked")
                    List<String> matchedSkills = (List<String>) workExp.get("matchedSkills");
                    if (matchedSkills != null && !matchedSkills.isEmpty()) {
                        resumeAnalysis.append("Work Experience Skills: ").append(String.join(", ", matchedSkills)).append("\n");
                    }
                }
            }
            
            // Add technical skills
            if (bestResume.containsKey("technicalSkills")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> techSkills = (Map<String, Object>) bestResume.get("technicalSkills");
                if (techSkills != null && techSkills.containsKey("matchedSkills")) {
                    @SuppressWarnings("unchecked")
                    List<String> matchedTechSkills = (List<String>) techSkills.get("matchedSkills");
                    if (matchedTechSkills != null && !matchedTechSkills.isEmpty()) {
                        resumeAnalysis.append("Technical Skills: ").append(String.join(", ", matchedTechSkills)).append("\n");
                    }
                }
            }
            
            // Add projects
            if (bestResume.containsKey("projects")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> projects = (Map<String, Object>) bestResume.get("projects");
                if (projects != null && projects.containsKey("matchedSkills")) {
                    @SuppressWarnings("unchecked")
                    List<String> projectSkills = (List<String>) projects.get("matchedSkills");
                    if (projectSkills != null && !projectSkills.isEmpty()) {
                        resumeAnalysis.append("Project Experience: ").append(String.join(", ", projectSkills)).append("\n");
                    }
                }
            }
            
            // Add certifications
            if (bestResume.containsKey("certificates")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> certificates = (Map<String, Object>) bestResume.get("certificates");
                if (certificates != null && certificates.containsKey("matchedSkills")) {
                    @SuppressWarnings("unchecked")
                    List<String> certSkills = (List<String>) certificates.get("matchedSkills");
                    if (certSkills != null && !certSkills.isEmpty()) {
                        resumeAnalysis.append("Certifications: ").append(String.join(", ", certSkills)).append("\n");
                    }
                }
            }
            
            // Add career summary
            if (bestResume.containsKey("careerSummary")) {
                @SuppressWarnings("unchecked")
                List<String> careerSummary = (List<String>) bestResume.get("careerSummary");
                if (careerSummary != null && !careerSummary.isEmpty()) {
                    resumeAnalysis.append("Career Summary: ").append(String.join(", ", careerSummary)).append("\n");
                }
            }
            
            String prompt = "Based on the following MATCHED resume analysis data, provide a focused explanation of why THIS SPECIFIC RESUME was selected as the best match. " +
                          "This is the WINNING resume that got selected, so highlight only its strengths and qualifications.\n\n" +
                          resumeAnalysis.toString() + "\n" +
                          "Format your response as follows:\n" +
                          "RESUME WAS SELECTED:\n" +
                          "===================\n\n" +
                          "Key strengths that made the difference:\n" +
                          "• [List 3-5 key strengths that made this resume stand out]\n\n" +
                          "Relevant work experience:\n" +
                          "• [Highlight 2-3 most relevant work experience points]\n\n" +
                          "Technical competencies:\n" +
                          "• [List 2-3 key technical skills that matched the job]\n\n" +
                          "Why this resume won:\n" +
                          "[Brief 2-3 sentence explanation of why this specific resume was chosen over others]\n\n" +
                          "Focus ONLY on this matched resume's qualifications. Do not mention other resumes or comparisons. " +
                          "Keep it concise and professional for Excel display.";

            String currentApiKey = getApiKey();
            if (currentApiKey == null || currentApiKey.equals("your-openai-api-key-here")) {
                throw new RuntimeException("OpenAI API key not configured");
            }
            String aiResponse = openAIUtils.callOpenAI(currentApiKey, prompt);
            
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                // Clean up the response
                String explanation = aiResponse.trim();
                
                // Remove any unwanted introductory phrases
                explanation = explanation.replaceFirst("^(Here is the analysis:|Analysis:|Resume Analysis:|This resume was selected because:)", "").trim();
                
                // Ensure it starts with the proper header for matched resume
                if (!explanation.startsWith("RESUME WAS SELECTED:")) {
                    explanation = "RESUME WAS SELECTED:\n===================\n\n" + explanation;
                }
                
                // Remove any mentions of other resumes or comparisons
                explanation = explanation.replaceAll("(?i)(compared to other|among all|other candidates|other resumes)", "among all candidates");
                explanation = explanation.replaceAll("(?i)(this candidate|this person)", "this resume");
                
                // Limit length for Excel cell
                if (explanation.length() > 3000) {
                    explanation = explanation.substring(0, 2997) + "...";
                }
                
                return explanation;
            }
            
            return "Analysis not available";
            
        } catch (Exception e) {
            System.out.println("DEBUG - Error generating AI ATS result: " + e.getMessage());
            return "Analysis data unavailable";
        }
    }
    
    /**
     * Generate comprehensive summary of all resume scores and results for a specific job description
     */
    private String generateAllResumeScoresSummary(List<Map<String, Object>> allResults, int jdIndex) {
        StringBuilder summary = new StringBuilder();
        
        try {
            summary.append("ALL RESUMES FOR THIS JOB:\n");
            summary.append("========================\n");
            
            int resumeCount = 0;
            for (Map<String, Object> resumeResult : allResults) {
                if (!resumeResult.containsKey("error")) {
                    // Check if this resume has matches for the specific JD
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> allMatches = (List<Map<String, Object>>) resumeResult.get("allMatches");
                    if (allMatches != null) {
                        for (Map<String, Object> match : allMatches) {
                            if (match.get("jdIndex") != null && match.get("jdIndex").equals(jdIndex)) {
                                resumeCount++;
                                
                                // Get resume name
                                String resumeName = (String) match.get("resumeName");
                                if (resumeName == null) {
                                    resumeName = "Unknown Resume";
                                }
                                
                                // Get ATS score
                                Double score = 0.0;
                                if (match.get("atsScore") != null) {
                                    try {
                                        @SuppressWarnings("unchecked")
                                        List<Double> scores = (List<Double>) match.get("atsScore");
                                        score = scores.get(0);
                                    } catch (Exception e) {
                                        score = 0.0;
                                    }
                                }
                                
                                // Determine match status based on position (first = matched)
                                String matchStatus = (resumeCount == 1) ? "MATCHED" : "NOT MATCHED";
                                
                                // Format the resume entry
                                summary.append(String.format("Resume %d: %s | Score: %.1f | Status: %s\n", 
                                    resumeCount, resumeName, score, matchStatus));
                                
                                // Add brief strengths if available
                                if (match.containsKey("strengths")) {
                                    @SuppressWarnings("unchecked")
                                    List<String> strengths = (List<String>) match.get("strengths");
                                    if (strengths != null && !strengths.isEmpty()) {
                                        String topStrength = strengths.get(0);
                                        if (topStrength.length() > 50) {
                                            topStrength = topStrength.substring(0, 47) + "...";
                                        }
                                        summary.append(String.format("  → Key Strength: %s\n", topStrength));
                                    }
                                }
                                
                                summary.append("\n");
                                break; // Only process the first match for this resume
                            }
                        }
                    }
                }
            }
            
            if (resumeCount == 0) {
                summary.append("No resumes processed for this job description.\n");
            } else {
                summary.append(String.format("Total Resumes Analyzed: %d\n", resumeCount));
            }
            
            // Clean up the summary and limit length
            String result = summary.toString();
            if (result.length() > 2000) {
                result = result.substring(0, 1997) + "...";
            }
            
            return result.isEmpty() ? "No resume data available" : result;
            
        } catch (Exception e) {
            System.out.println("DEBUG - Error generating all resume scores summary: " + e.getMessage());
            return "Error generating resume summary";
        }
    }
}


