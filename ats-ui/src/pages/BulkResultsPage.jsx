import React, { useState } from "react";
import ResumeScoreCircle from "../components/ResumeScoreCircle";
import ActionableFeedback from "../components/ActionableFeedback";

const BulkResultsPage = ({ data, mode = 3 }) => {
  const [expandedResume, setExpandedResume] = useState(null);

  // Extract data from the API response
  const summary = data?.summary || {};
  const matchedResults = data?.matchedResults || [];
  const unmatchedResults = data?.unmatchedResults || [];
  const jobDescription = data?.jobDescription || "";

  const toggleResumeExpansion = (index) => {
    setExpandedResume(expandedResume === index ? null : index);
  };

  const getScoreColor = (score) => {
    if (score >= 8) return "#10B981"; // green
    if (score >= 6) return "#F59E0B"; // yellow
    return "#EF4444"; // red
  };

  const getRankBadge = (index) => {
    if (index === 0) return "🥇 1st";
    if (index === 1) return "🥈 2nd";
    if (index === 2) return "🥉 3rd";
    return `#${index + 1}`;
  };

  // Function to download renamed resume
  const handleDownloadResume = async (originalResumeName, newResumeName) => {
    try {
      console.log("Starting download for:", originalResumeName, "->", newResumeName);
      
      // Find the resume data to get the original content
      console.log("DEBUG - Looking for resume data:", { originalResumeName, newResumeName });
      console.log("DEBUG - Available matchedResults:", matchedResults);
      
      const resumeData = matchedResults.find(result => 
        result.resumeName === originalResumeName && result.newResumeName === newResumeName
      );
      
      console.log("DEBUG - Found resume data:", resumeData);
      
      if (!resumeData || !resumeData.originalResumeContent) {
        console.error("DEBUG - Resume content missing:", { 
          hasResumeData: !!resumeData, 
          hasContent: resumeData ? !!resumeData.originalResumeContent : false,
          contentLength: resumeData?.originalResumeContent?.length || 0
        });
        throw new Error("Resume content not found");
      }
      
      // Send JSON data to backend
      const requestBody = {
        resumeName: originalResumeName,
        newResumeName: newResumeName,
        originalResumeContent: resumeData.originalResumeContent
      };
      
      console.log("DEBUG - Sending request to backend:", {
        url: '/api/download-resume',
        method: 'POST',
        bodySize: JSON.stringify(requestBody).length,
        contentLength: resumeData.originalResumeContent.length
      });
      
      const response = await fetch('/api/download-resume', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      });
      
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
      alert("Error downloading resume. Please try again.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="p-6 max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-800">Bulk Resume Analysis</h1>
          <div className="text-sm text-gray-600 bg-white px-4 py-2 rounded-lg shadow">
            Mode {mode}: Multiple Resumes vs Job Description
          </div>
        </div>

        {/* Summary Section */}
        <div className="bg-white rounded-xl shadow p-6 mb-8">
          <h2 className="text-2xl font-bold mb-4">📊 Analysis Summary</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="text-center">
              <div className="text-3xl font-bold text-blue-600">{summary.totalResumes || 0}</div>
              <div className="text-sm text-gray-600">Total Resumes</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-green-600">{summary.matchedResumes || 0}</div>
              <div className="text-sm text-gray-600">Matched Resumes</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-red-600">{summary.unmatchedResumes || 0}</div>
              <div className="text-sm text-gray-600">Unmatched Resumes</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-purple-600">{summary.averageScore || 0}</div>
              <div className="text-sm text-gray-600">Average Score</div>
            </div>
          </div>
        </div>

        {/* Best Match Highlight */}
        {summary.bestMatch && (
          <div className="bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-xl p-6 mb-8">
            <h3 className="text-xl font-bold text-green-800 mb-4">🏆 Best Match</h3>
            <div className="flex items-center gap-4">
              <ResumeScoreCircle score={summary.bestMatch.atsScore?.[0] || 0} />
              <div className="flex-1">
                <h4 className="text-lg font-semibold text-green-800">
                  {summary.bestMatch.resumeName}
                </h4>
                <p className="text-green-700">
                  Score: {summary.bestMatch.atsScore?.[0] || 0}/10
                </p>
                <p className="text-sm text-green-600 mt-2">
                  {summary.bestMatch.careerSummary?.[0] || "No summary available"}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Summary Note */}
        <div className="bg-green-50 border border-green-200 rounded-xl p-6 text-center">
          <p className="text-green-700">
            💡 <strong>Note:</strong> Only resumes with ATS scores ≥ 6 are shown above. 
            Each matched resume has been renamed according to the job description and can be downloaded.
          </p>
        </div>
        
        {/* Matched Resumes with New Names */}
        {matchedResults.length > 0 && (
          <div className="bg-white rounded-xl shadow p-6 mb-8">
            <h3 className="text-xl font-bold text-gray-800 mb-4">✅ Matched Resumes</h3>
            <div className="space-y-4">
              {matchedResults.map((result, index) => (
                <div key={index} className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <h4 className="text-lg font-semibold text-gray-800 mb-2">
                        {result.resumeName}
                      </h4>
                      {result.newResumeName && (
                        <div className="space-y-2">
                          <p className="text-sm text-blue-600 font-medium">
                            📝 New Name: {result.newResumeName}
                          </p>
                          <button 
                            className="bg-green-500 hover:bg-green-600 text-white px-3 py-1 rounded text-xs font-medium transition-colors"
                            onClick={() => handleDownloadResume(result.resumeName, result.newResumeName)}
                          >
                            📥 Download Renamed Resume
                          </button>
                        </div>
                      )}
                      <p className="text-sm text-gray-600 mt-2">
                        Score: <span className="font-semibold" style={{ color: getScoreColor(result.atsScore?.[0] || 0) }}>
                          {result.atsScore?.[0] || 0}/10
                        </span>
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Job Description */}
        <div className="mt-8 bg-white rounded-xl shadow p-6">
          <h3 className="text-xl font-bold text-gray-800 mb-4">📄 Job Description</h3>
          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-gray-700 whitespace-pre-wrap">{jobDescription}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default BulkResultsPage;

