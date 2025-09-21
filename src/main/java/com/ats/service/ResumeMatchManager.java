package com.ats.service;

import com.ats.model.ResumeMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing resume matches and file storage
 */
@Service
public class ResumeMatchManager {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private RenamedFileStorageService renamedFileStorageService;

    // Store matches by JD index
    private final ConcurrentHashMap<Integer, List<ResumeMatch>> matchesByJD = new ConcurrentHashMap<>();

    /**
     * Store a resume match with physical file storage AND create renamed version
     */
    public void storeResumeMatch(int jdIndex, String jobDescription, String resumeFileName, 
                                String originalResumeName, double matchScore, MultipartFile resumeFile) {
        try {
            System.out.println("🔄 STORING RESUME MATCH - Starting process...");
            System.out.println("   JD Index: " + jdIndex);
            System.out.println("   Resume: " + resumeFileName);
            System.out.println("   Job Description length: " + (jobDescription != null ? jobDescription.length() : "null"));
            
            // Store the resume file physically
            String storedFilename = fileStorageService.storeResume(resumeFile);
            System.out.println("✅ Original file stored: " + storedFilename);

            // Create renamed version automatically
            String renamedFilename = null;
            try {
                System.out.println("🔄 Attempting to create renamed file...");
                
                // Extract company and role from job description (basic extraction)
                String companyName = extractCompanyFromJD(jobDescription);
                String roleName = extractRoleFromJD(jobDescription);
                
                System.out.println("   Extracted Company: '" + companyName + "'");
                System.out.println("   Extracted Role: '" + roleName + "'");
                
                // Create renamed file
                renamedFilename = renamedFileStorageService.storeRenamedResume(resumeFile, companyName, roleName, null);
                
                System.out.println("✅ Renamed file created: " + renamedFilename);
                System.out.println("   Company: " + companyName);
                System.out.println("   Role: " + roleName);
                
            } catch (Exception e) {
                System.err.println("⚠️ Failed to create renamed file: " + e.getMessage());
                e.printStackTrace();
                // Continue even if renamed file creation fails
            }

            ResumeMatch match = new ResumeMatch(
                jobDescription,
                resumeFileName,
                storedFilename,
                matchScore,
                originalResumeName
            );
            
            // Store additional info in the match
            if (renamedFilename != null) {
                match.setCompanyName(extractCompanyFromJD(jobDescription));
                match.setRoleName(extractRoleFromJD(jobDescription));
                match.setFileType(getFileType(resumeFileName));
            }

            matchesByJD.computeIfAbsent(jdIndex, k -> new ArrayList<>()).add(match);
            
            System.out.println("✅ Resume match stored for JD " + jdIndex);
            System.out.println("   Resume: " + resumeFileName);
            System.out.println("   Stored as: " + storedFilename);
            System.out.println("   Renamed as: " + renamedFilename);
            System.out.println("   Score: " + matchScore);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to store resume match: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract company name from job description
     */
    private String extractCompanyFromJD(String jobDescription) {
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return "UnknownCompany";
        }
        
        // Simple extraction - look for common patterns
        String jd = jobDescription.toLowerCase();
        
        // Look for company indicators
        if (jd.contains("google")) return "Google";
        if (jd.contains("microsoft")) return "Microsoft";
        if (jd.contains("amazon")) return "Amazon";
        if (jd.contains("apple")) return "Apple";
        if (jd.contains("facebook") || jd.contains("meta")) return "Meta";
        if (jd.contains("netflix")) return "Netflix";
        if (jd.contains("tesla")) return "Tesla";
        if (jd.contains("uber")) return "Uber";
        if (jd.contains("airbnb")) return "Airbnb";
        if (jd.contains("rolls royce") || jd.contains("rolls-royce")) return "RollsRoyce";
        
        // Try to extract from the beginning of the description
        String[] lines = jobDescription.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 0 && !line.toLowerCase().contains("job description") && 
                !line.toLowerCase().contains("job title") && !line.toLowerCase().contains("working pattern")) {
                // This might be a company name
                String potentialCompany = line.replaceAll("[^a-zA-Z0-9\\s\\-_]", "").trim();
                if (potentialCompany.length() > 2 && potentialCompany.length() < 50) {
                    return potentialCompany.replaceAll("\\s+", "");
                }
            }
        }
        
        // Default company name
        return "Company_" + System.currentTimeMillis() % 1000;
    }

    /**
     * Extract role name from job description
     */
    private String extractRoleFromJD(String jobDescription) {
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return "UnknownRole";
        }
        
        // Simple extraction - look for common role patterns
        String jd = jobDescription.toLowerCase();
        
        if (jd.contains("software engineer") || jd.contains("developer")) return "SoftwareEngineer";
        if (jd.contains("data scientist")) return "DataScientist";
        if (jd.contains("product manager")) return "ProductManager";
        if (jd.contains("designer") || jd.contains("ux")) return "UXDesigner";
        if (jd.contains("analyst")) return "DataAnalyst";
        if (jd.contains("manager")) return "Manager";
        if (jd.contains("lead")) return "TechLead";
        if (jd.contains("architect")) return "Architect";
        if (jd.contains("hr people partner") || jd.contains("hr partner")) return "HRPeoplePartner";
        if (jd.contains("labor relations")) return "LaborRelations";
        
        // Try to extract from job title line
        String[] lines = jobDescription.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.toLowerCase().contains("job title:") || line.toLowerCase().contains("role:")) {
                String role = line.substring(line.indexOf(":") + 1).trim();
                if (role.length() > 0) {
                    return role.replaceAll("[^a-zA-Z0-9\\s\\-_]", "").replaceAll("\\s+", "");
                }
            }
        }
        
        // Default role name
        return "Role_" + System.currentTimeMillis() % 1000;
    }

    /**
     * Get file type from filename
     */
    private String getFileType(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "application/octet-stream";
        }
        
        String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        switch (extension) {
            case ".pdf": return "application/pdf";
            case ".doc": return "application/msword";
            case ".docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".txt": return "text/plain";
            default: return "application/octet-stream";
        }
    }

    /**
     * Get all matches for a specific JD
     */
    public List<ResumeMatch> getMatchesForJD(int jdIndex) {
        return matchesByJD.getOrDefault(jdIndex, new ArrayList<>());
    }

    /**
     * Get the best match for a specific JD
     */
    public ResumeMatch getBestMatchForJD(int jdIndex) {
        List<ResumeMatch> matches = getMatchesForJD(jdIndex);
        if (matches.isEmpty()) {
            return null;
        }
        
        // Return the match with highest score
        return matches.stream()
                .max((m1, m2) -> Double.compare(m1.getMatchScore(), m2.getMatchScore()))
                .orElse(null);
    }

    /**
     * Get all stored matches
     */
    public ConcurrentHashMap<Integer, List<ResumeMatch>> getAllMatches() {
        return matchesByJD;
    }

    /**
     * Clear all matches
     */
    public void clearAllMatches() {
        // Delete all stored files
        matchesByJD.values().stream()
                .flatMap(List::stream)
                .forEach(match -> {
                    if (match.getStoredResumeFilename() != null) {
                        fileStorageService.deleteFile(match.getStoredResumeFilename());
                    }
                });
        
        matchesByJD.clear();
        System.out.println("✅ All resume matches cleared and files deleted");
    }

    /**
     * Get storage directory info
     */
    public String getStorageInfo() {
        return "Storage directory: " + fileStorageService.getStorageLocation().toAbsolutePath();
    }

    // Additional methods for ResumeMatchController compatibility

    /**
     * Set resume content for a specific match
     */
    public boolean setResumeContent(String matchId, byte[] resumeFile) {
        // Find match by ID across all JDs
        for (List<ResumeMatch> matches : matchesByJD.values()) {
            for (ResumeMatch match : matches) {
                if (matchId.equals(match.getMatchId())) {
                    // Note: This is a placeholder - actual files are stored on disk
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Download best match for a specific JD
     */
    public byte[] downloadBestMatchForJD(int jdIndex) throws IOException {
        ResumeMatch bestMatch = getBestMatchForJD(jdIndex);
        if (bestMatch == null) {
            throw new IllegalStateException("No best match found for JD: " + jdIndex);
        }
        
        // Get file from storage
        Path filePath = fileStorageService.getStoredFilePath(bestMatch.getStoredResumeFilename());
        if (!Files.exists(filePath)) {
            throw new IOException("Stored file not found: " + bestMatch.getStoredResumeFilename());
        }
        
        return Files.readAllBytes(filePath);
    }

    /**
     * Get all best matches across all JDs
     */
    public List<ResumeMatch> getAllBestMatches() {
        List<ResumeMatch> bestMatches = new ArrayList<>();
        for (Integer jdIndex : matchesByJD.keySet()) {
            ResumeMatch bestMatch = getBestMatchForJD(jdIndex);
            if (bestMatch != null) {
                bestMatches.add(bestMatch);
            }
        }
        return bestMatches;
    }

    /**
     * Get matched resumes (score >= 6.0)
     */
    public List<ResumeMatch> getMatchedResumes() {
        return matchesByJD.values().stream()
                .flatMap(List::stream)
                .filter(match -> match.getMatchScore() >= 6.0)
                .collect(Collectors.toList());
    }

    /**
     * Get unmatched resumes (score < 6.0)
     */
    public List<ResumeMatch> getUnmatchedResumes() {
        return matchesByJD.values().stream()
                .flatMap(List::stream)
                .filter(match -> match.getMatchScore() < 6.0)
                .collect(Collectors.toList());
    }

    /**
     * Get resume match by ID
     */
    public ResumeMatch getResumeMatch(String matchId) {
        for (List<ResumeMatch> matches : matchesByJD.values()) {
            for (ResumeMatch match : matches) {
                if (matchId.equals(match.getMatchId())) {
                    return match;
                }
            }
        }
        return null;
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        int totalMatches = matchesByJD.values().stream()
                .mapToInt(List::size)
                .sum();
        
        int matchedCount = getMatchedResumes().size();
        int unmatchedCount = getUnmatchedResumes().size();
        
        stats.put("totalMatches", totalMatches);
        stats.put("matchedResumes", matchedCount);
        stats.put("unmatchedResumes", unmatchedCount);
        stats.put("totalJobDescriptions", matchesByJD.size());
        
        return stats;
    }

    /**
     * Search resume matches
     */
    public List<ResumeMatch> searchResumeMatches(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String searchQuery = query.toLowerCase().trim();
        
        return matchesByJD.values().stream()
                .flatMap(List::stream)
                .filter(match -> 
                    (match.getJobDescription() != null && 
                     match.getJobDescription().toLowerCase().contains(searchQuery)) ||
                    (match.getResumeFileName() != null && 
                     match.getResumeFileName().toLowerCase().contains(searchQuery)) ||
                    (match.getOriginalResumeName() != null && 
                     match.getOriginalResumeName().toLowerCase().contains(searchQuery))
                )
                .collect(Collectors.toList());
    }

    /**
     * Get matches by score range
     */
    public List<ResumeMatch> getMatchesByScoreRange(double minScore, double maxScore) {
        return matchesByJD.values().stream()
                .flatMap(List::stream)
                .filter(match -> match.getMatchScore() >= minScore && match.getMatchScore() <= maxScore)
                .collect(Collectors.toList());
    }
}
