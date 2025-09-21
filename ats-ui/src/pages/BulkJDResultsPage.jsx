import React, { useState } from 'react';
import ATSResultCard from '../components/ATSResultCard';
import './BulkJDResultsPage.css';

const BulkJDResultsPage = ({ data }) => {
  console.log("BulkJDResultsPage - Received data:", data);
  
  // Debug: Log the structure of the data
  if (data && data.resumeResults) {
    console.log("DEBUG - Data structure analysis:");
    data.resumeResults.forEach((resume, index) => {
      console.log(`  Resume ${index}:`, {
        resumeName: resume.resumeName,
        newResumeName: resume.newResumeName,
        hasContent: !!resume.originalResumeContent,
        contentLength: resume.originalResumeContent?.length || 0,
        allMatches: resume.allMatches?.length || 0
      });
      
      if (resume.allMatches) {
        resume.allMatches.forEach((match, matchIndex) => {
          console.log(`    Match ${matchIndex}:`, {
            resumeName: match.resumeName,
            newResumeName: match.newResumeName,
            hasContent: !!match.originalResumeContent,
            contentLength: match.originalResumeContent?.length || 0,
            jdIndex: match.jdIndex
          });
        });
      }
    });
  }
  
  const [expandedResume, setExpandedResume] = useState(null);
  const [sortBy, setSortBy] = useState('score'); // 'score', 'name', 'jd'

  // Function to download Excel file
  const downloadExcel = (excelData, fileName) => {
    try {
      // Convert base64 string to blob
      const byteCharacters = atob(excelData);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      const blob = new Blob([byteArray], { 
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
      });
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName || 'JD_Analysis_Results.xlsx';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      console.log("Excel file downloaded successfully");
    } catch (error) {
      console.error("Error downloading Excel file:", error);
      alert("Error downloading Excel file. Please try again.");
    }
  };

  // Function to find best resume for a specific JD
  const findBestResumeForJD = (resumeResults, jdIndex) => {
    let bestResume = null;
    let highestScore = -1;
    let parentResume = null;
    
    for (const resumeResult of resumeResults) {
      if (!resumeResult.error) {
        const allMatches = resumeResult.allMatches;
        if (allMatches) {
          for (const match of allMatches) {
            if (match.jdIndex === jdIndex) {
              const score = match.atsScore[0];
              if (score > highestScore) {
                highestScore = score;
                bestResume = match;
                parentResume = resumeResult; // Store the parent resume that contains the content
              }
            }
          }
        }
      }
    }
    
    // If we found a best resume, merge it with the parent resume data to get the content
    if (bestResume && parentResume) {
      const mergedResume = { ...bestResume };
      // Copy the resume content from the parent
      if (parentResume.originalResumeContent) {
        mergedResume.originalResumeContent = parentResume.originalResumeContent;
        mergedResume.originalResumeName = parentResume.originalResumeName;
        console.log("DEBUG - Merged resume content from parent:", {
          resumeName: mergedResume.resumeName,
          newResumeName: mergedResume.newResumeName,
          hasContent: !!mergedResume.originalResumeContent,
          contentLength: mergedResume.originalResumeContent?.length || 0
        });
      } else {
        console.log("DEBUG - Parent resume has no content:", {
          resumeName: parentResume.resumeName,
          hasContent: !!parentResume.originalResumeContent,
          contentLength: parentResume.originalResumeContent?.length || 0
        });
      }
      return mergedResume;
    }
    
    return bestResume;
  };

  if (!data || !data.resumeResults) {
    console.log("BulkJDResultsPage - No data or resumeResults:", { data, resumeResults: data?.resumeResults });
    return <div className="error-container">No data available</div>;
  }

  const { summary, resumeResults, totalJobDescriptions } = data;

  const handleResumeExpand = (resumeIndex) => {
    setExpandedResume(expandedResume === resumeIndex ? null : resumeIndex);
  };

  const getSortedResults = () => {
    const sorted = [...resumeResults];
    switch (sortBy) {
      case 'score':
        return sorted.sort((a, b) => {
          const scoreA = a.atsScore ? a.atsScore[0] : 0;
          const scoreB = b.atsScore ? b.atsScore[0] : 0;
          return scoreB - scoreA; // Descending
        });
      case 'name':
        return sorted.sort((a, b) => a.resumeName.localeCompare(b.resumeName));
      case 'jd':
        return sorted.sort((a, b) => a.jdIndex - b.jdIndex);
      default:
        return sorted;
    }
  };

  const getScoreColor = (score) => {
    if (score >= 8) return '#4CAF50';
    if (score >= 6) return '#FF9800';
    return '#F44336';
  };

  // Function to download renamed resume
  const downloadResume = async (jdIndex, originalResumeName, newResumeName) => {
    try {
      console.log("DEBUG - Starting download for JD:", jdIndex);
      console.log("DEBUG - Original resume name:", originalResumeName);
      console.log("DEBUG - New resume name:", newResumeName);
      
      // Use the correct endpoint for downloading best matches
      const response = await fetch(`/api/resume-matches/job-description/${jdIndex}/download-best`);
      
      console.log("DEBUG - Response received:", { 
        status: response.status, 
        ok: response.ok, 
        statusText: response.statusText 
      });
      
      if (response.ok) {
        // Get the file content as blob
        const fileBlob = await response.blob();
        console.log("DEBUG - File blob received:", { 
          size: fileBlob.size, 
          type: fileBlob.type 
        });
        
        // Create download link
        const url = window.URL.createObjectURL(fileBlob);
        const link = document.createElement('a');
        link.href = url;
        
        // Set filename with proper extension
        const fileExtension = originalResumeName.includes('.') ? 
          originalResumeName.substring(originalResumeName.lastIndexOf('.')) : '.pdf';
        link.download = `${newResumeName}${fileExtension}`;
        
        console.log("DEBUG - Triggering download:", { 
          filename: `${newResumeName}${fileExtension}`,
          url: url 
        });
        
        // Trigger download
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        console.log("Resume downloaded successfully:", newResumeName);
      } else {
        const errorText = await response.text();
        console.error("DEBUG - Download failed:", { 
          status: response.status, 
          statusText: response.statusText,
          errorText: errorText 
        });
        throw new Error(`Download failed: ${response.status} - ${errorText}`);
      }
    } catch (error) {
      console.error("Error downloading resume:", error);
      alert(`Download failed: ${error.message}`);
    }
  };

  return (
    <div className="bulk-jd-results">
      <div className="results-header">
        <h1>Bulk Job Description Analysis Results</h1>
        <p className="results-subtitle">
          {resumeResults.length} resumes analyzed against {totalJobDescriptions} job descriptions
        </p>
      </div>

      {/* Summary Section */}
      <div className="summary-section">
        <div className="summary-grid">
          <div className="summary-card">
            <h3>Total Resumes</h3>
            <div className="summary-value">{summary.totalResumes}</div>
          </div>
          <div className="summary-card">
            <h3>Total Job Descriptions</h3>
            <div className="summary-value">{summary.totalJobDescriptions}</div>
          </div>
          <div className="summary-card">
            <h3>Matched Resumes</h3>
            <div className="summary-value" style={{ color: '#4CAF50' }}>{data.totalMatched || 0}</div>
          </div>
          <div className="summary-card">
            <h3>Unmatched Resumes</h3>
            <div className="summary-value" style={{ color: '#F44336' }}>{data.totalUnmatched || 0}</div>
          </div>
        </div>
      </div>

      {/* Controls */}
      <div className="controls-section">
        <div className="sort-controls">
          <label>Sort by:</label>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="score">Score (Highest First)</option>
            <option value="name">Resume Name</option>
            <option value="jd">Job Description Order</option>
          </select>
        </div>
        
        {/* Download Excel Button */}
        {data.excelData && (
          <div className="download-controls">
            <button 
              className="download-excel-btn"
              onClick={() => downloadExcel(data.excelData, data.excelFileName)}
            >
              📥 Download Excel Results
            </button>
            <p className="download-info">
              Excel file contains best match resume for each job description
            </p>
          </div>
        )}
      </div>

      {/* Best Overall Match */}
      {summary.bestOverallMatch && (
        <div className="best-match-section">
          <h2>🏆 Best Overall Match</h2>
          <div className="best-match-card">
            <div className="best-match-info">
              <h3>{summary.bestOverallMatch.resumeName}</h3>
              <p>Job Description #{summary.bestOverallMatch.jdIndex + 1}</p>
              <div className="best-match-score">
                Score: <span style={{ color: getScoreColor(summary.bestOverallMatch.atsScore[0]) }}>
                  {summary.bestOverallMatch.atsScore[0]}/10
                </span>
              </div>
            </div>
            <div className="best-match-preview">
              <p>{summary.bestOverallMatch.jdText}</p>
            </div>
          </div>
        </div>
      )}

             {/* JD Summary Table */}
       <div className="jd-summary-section">
         <h2>📊 Job Description Summary</h2>
         <div className="jd-summary-table">
           <div className="jd-summary-header">
             <div className="jd-header-cell">Job Description</div>
             <div className="jd-header-cell">Best Match Resume</div>
             <div className="jd-header-cell">New Resume Name</div>
             <div className="jd-header-cell">ATS Score</div>
             <div className="jd-header-cell">Action</div>
           </div>
           {Array.from({ length: totalJobDescriptions }, (_, jdIndex) => {
             const bestResume = findBestResumeForJD(resumeResults, jdIndex);
             return (
               <div key={jdIndex} className="jd-summary-row">
                 <div className="jd-cell">JD #{jdIndex + 1}</div>
                 <div className="jd-cell">
                   {bestResume ? bestResume.resumeName : 'No match found'}
                 </div>
                 <div className="jd-cell">
                   {bestResume && bestResume.newResumeName ? (
                     <span className="new-resume-name-inline">
                       {bestResume.newResumeName}
                     </span>
                   ) : (
                     'No rename'
                   )}
                 </div>
                 <div className="jd-cell score">
                   {bestResume ? (
                     <span style={{ color: getScoreColor(bestResume.atsScore[0]) }}>
                       {bestResume.atsScore[0]}/10
                     </span>
                   ) : (
                     '0/10'
                   )}
                 </div>
                                   <div className="jd-cell">
                    {bestResume && bestResume.newResumeName ? (
                      <button 
                        className="download-btn-inline"
                        onClick={() => {
                          console.log("DEBUG - Download button clicked for:", {
                            resumeName: bestResume.resumeName,
                            newResumeName: bestResume.newResumeName,
                            hasContent: !!bestResume.originalResumeContent,
                            contentLength: bestResume.originalResumeContent?.length || 0,
                            jdIndex: bestResume.jdIndex
                          });
                          
                          // Debug: Check what bestResume contains
                          console.log("DEBUG - Full bestResume object:", bestResume);
                          
                          downloadResume(bestResume.jdIndex, bestResume.resumeName, bestResume.newResumeName);
                        }}
                      >
                        📥 Download
                      </button>
                    ) : (
                      '-'
                    )}
                  </div>
               </div>
             );
           })}
         </div>
       </div>

      {/* Summary Note */}
      <div className="summary-note">
        <p>💡 <strong>Note:</strong> Only resumes with ATS scores ≥ 6 are shown in the summary table above. 
        Each row shows the best matching resume for that job description, along with its new renamed filename.</p>
      </div>

    </div>
  );
};

export default BulkJDResultsPage;
