package com.ats.model;

/**
 * Model class for storing resume match information
 */
public class ResumeMatch {
    private String jobDescription;
    private String resumeFileName;
    private String storedResumeFilename; // Physical file reference
    private double matchScore;
    private String originalResumeName;
    
    // Additional fields for compatibility
    private String matchId;
    private String companyName;
    private String roleName;
    private String userName;
    private String fileType;

    public ResumeMatch() {}

    public ResumeMatch(String jobDescription, String resumeFileName, String storedResumeFilename, double matchScore, String originalResumeName) {
        this.jobDescription = jobDescription;
        this.resumeFileName = resumeFileName;
        this.storedResumeFilename = storedResumeFilename;
        this.matchScore = matchScore;
        this.originalResumeName = originalResumeName;
        this.matchId = java.util.UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public void setResumeFileName(String resumeFileName) {
        this.resumeFileName = resumeFileName;
    }

    public String getStoredResumeFilename() {
        return storedResumeFilename;
    }

    public void setStoredResumeFilename(String storedResumeFilename) {
        this.storedResumeFilename = storedResumeFilename;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public String getOriginalResumeName() {
        return originalResumeName;
    }

    public void setOriginalResumeName(String originalResumeName) {
        this.originalResumeName = originalResumeName;
    }

    // Additional getters for compatibility
    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    // Compatibility methods
    public String getResumeName() {
        return resumeFileName;
    }

    public String getNewResumeName() {
        return originalResumeName;
    }

    public double getAtsScore() {
        return matchScore;
    }

    public byte[] getResumeFile() {
        // Return empty array for compatibility - actual files are stored on disk
        return new byte[0];
    }

    @Override
    public String toString() {
        return "ResumeMatch{" +
                "jobDescription='" + jobDescription + '\'' +
                ", resumeFileName='" + resumeFileName + '\'' +
                ", storedResumeFilename='" + storedResumeFilename + '\'' +
                ", matchScore=" + matchScore +
                ", originalResumeName='" + originalResumeName + '\'' +
                ", matchId='" + matchId + '\'' +
                '}';
    }
}
