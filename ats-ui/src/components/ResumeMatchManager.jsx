import React, { useState, useEffect } from 'react';
import './ResumeMatchManager.css';

const ResumeMatchManager = () => {
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('all'); // all, matched, unmatched
  const [searchQuery, setSearchQuery] = useState('');
  const [statistics, setStatistics] = useState({});
  const [bestMatches, setBestMatches] = useState([]);

  useEffect(() => {
    fetchMatches();
    fetchStatistics();
    fetchBestMatches();
  }, []);

  const fetchMatches = async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/resume-matches');
      if (response.ok) {
        const data = await response.json();
        setMatches(data);
      } else {
        throw new Error('Failed to fetch matches');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchStatistics = async () => {
    try {
      const response = await fetch('/api/resume-matches/statistics');
      if (response.ok) {
        const data = await response.json();
        setStatistics(data);
      }
    } catch (err) {
      console.error('Failed to fetch statistics:', err);
    }
  };

  const fetchBestMatches = async () => {
    try {
      const response = await fetch('/api/resume-matches/best-matches');
      if (response.ok) {
        const data = await response.json();
        setBestMatches(data);
      }
    } catch (err) {
      console.error('Failed to fetch best matches:', err);
    }
  };

  const downloadBestMatch = async (jdIndex) => {
    try {
      console.log(`🚀 Starting download for JD ${jdIndex}...`);
      
      const response = await fetch(`/api/resume-matches/job-description/${jdIndex}/download-best`);
      console.log(`📡 Response received:`, response);
      console.log(`📊 Response status: ${response.status}`);
      console.log(`📋 Response headers:`, response.headers);
      
      if (response.ok) {
        console.log(`✅ Response OK, creating blob...`);
        const blob = await response.blob();
        console.log(`📦 Blob created:`, blob);
        console.log(`📏 Blob size: ${blob.size} bytes`);
        console.log(`🔤 Blob type: ${blob.type}`);
        
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        
        // Find the best match to get the new resume name and file type
        const bestMatch = bestMatches.find(match => match.jdIndex === jdIndex);
        console.log(`🎯 Best match found:`, bestMatch);
        
        if (bestMatch) {
          // Determine file extension based on content type
          const contentType = response.headers.get('content-type');
          console.log(`📄 Content type: ${contentType}`);
          
          let extension = '';
          if (contentType.includes('pdf')) extension = '.pdf';
          else if (contentType.includes('docx')) extension = '.docx';
          else if (contentType.includes('doc')) extension = '.doc';
          else if (contentType.includes('txt')) extension = '.txt';
          else extension = '';
          
          const filename = `${bestMatch.newResumeName}${extension}`;
          console.log(`📝 Setting download filename: ${filename}`);
          link.download = filename;
        } else {
          const filename = `BestMatch_JD${jdIndex + 1}`;
          console.log(`📝 No best match found, using default filename: ${filename}`);
          link.download = filename;
        }
        
        console.log(`🔗 Triggering download...`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        console.log(`✅ Download completed successfully!`);
      } else {
        console.error(`❌ Response not OK: ${response.status}`);
        const errorText = await response.text();
        console.error(`❌ Error text: ${errorText}`);
        alert(`Download failed: ${errorText}`);
      }
    } catch (err) {
      console.error(`💥 Download error:`, err);
      console.error(`💥 Error message: ${err.message}`);
      console.error(`💥 Error stack:`, err.stack);
      alert(`Download failed: ${err.message}`);
    }
  };

  const clearAllMatches = async () => {
    if (window.confirm('Are you sure you want to clear all matches? This action cannot be undone.')) {
      try {
        const response = await fetch('/api/resume-matches', { method: 'DELETE' });
        if (response.ok) {
          setMatches([]);
          setBestMatches([]);
          fetchStatistics();
          alert('All matches cleared successfully');
        } else {
          throw new Error('Failed to clear matches');
        }
      } catch (err) {
        alert('Failed to clear matches: ' + err.message);
      }
    }
  };

  const getFilteredMatches = () => {
    let filtered = matches;

    // Apply status filter
    if (filter === 'matched') {
      filtered = filtered.filter(match => match.matchStatus === 'MATCHED');
    } else if (filter === 'unmatched') {
      filtered = filtered.filter(match => match.matchStatus === 'UNMATCHED');
    }

    // Apply search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(match =>
        match.resumeName.toLowerCase().includes(query) ||
        match.companyName.toLowerCase().includes(query) ||
        match.roleName.toLowerCase().includes(query) ||
        match.jobDescription.toLowerCase().includes(query)
      );
    }

    return filtered.sort((a, b) => b.atsScore - a.atsScore);
  };

  const getScoreColor = (score) => {
    if (score >= 8) return '#4CAF50';
    if (score >= 6) return '#FF9800';
    return '#F44336';
  };

  const getStatusBadge = (status) => {
    const color = status === 'MATCHED' ? '#4CAF50' : '#F44336';
    return (
      <span className="status-badge" style={{ backgroundColor: color }}>
        {status}
      </span>
    );
  };

  if (loading) {
    return <div className="loading">Loading resume matches...</div>;
  }

  if (error) {
    return <div className="error">Error: {error}</div>;
  }

  const filteredMatches = getFilteredMatches();

  return (
    <div className="resume-match-manager">
      <div className="manager-header">
        <h1>📋 Resume Match Manager</h1>
        <p>View bulk resume analysis results and download best matches for job descriptions</p>
        
        {/* Test Download Button */}
        <button 
          className="test-download-btn" 
          onClick={() => testDownload()}
          style={{
            marginTop: '10px',
            padding: '8px 16px',
            backgroundColor: '#28a745',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          🧪 Test Download (Debug)
        </button>
      </div>

      {/* Statistics */}
      <div className="statistics-section">
        <div className="stats-grid">
          <div className="stat-card">
            <h3>Total Matches</h3>
            <div className="stat-value">{statistics.totalMatches || 0}</div>
          </div>
          <div className="stat-card">
            <h3>Matched Resumes</h3>
            <div className="stat-value" style={{ color: '#4CAF50' }}>
              {statistics.matchedResumes || 0}
            </div>
          </div>
          <div className="stat-card">
            <h3>Unmatched Resumes</h3>
            <div className="stat-value" style={{ color: '#F44336' }}>
              {statistics.unmatchedResumes || 0}
            </div>
          </div>
          <div className="stat-card">
            <h3>Job Descriptions</h3>
            <div className="stat-value">{statistics.uniqueJobDescriptions || 0}</div>
          </div>
        </div>
      </div>

      {/* Best Matches Section */}
      {bestMatches.length > 0 && (
        <div className="best-matches-section">
          <h2>🏆 Best Matches for Each Job Description</h2>
          <div className="best-matches-grid">
            {bestMatches.map((match) => (
              <div key={match.matchId} className="best-match-card">
                <div className="best-match-header">
                  <h3>JD #{match.jdIndex + 1}</h3>
                  <span className="best-match-score" style={{ color: getScoreColor(match.atsScore) }}>
                    {match.atsScore}/10
                  </span>
                </div>
                <div className="best-match-details">
                  <p><strong>Company:</strong> {match.companyName}</p>
                  <p><strong>Role:</strong> {match.roleName}</p>
                  <p><strong>Best Resume:</strong> {match.resumeName}</p>
                </div>
                <button
                  className="download-best-btn"
                  onClick={() => downloadBestMatch(match.jdIndex)}
                >
                  📥 Download Best Match
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Controls */}
      <div className="controls-section">
        <div className="filters">
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="all">All Matches</option>
            <option value="matched">Matched Only</option>
            <option value="unmatched">Unmatched Only</option>
          </select>
          
          <input
            type="text"
            placeholder="Search matches..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
        </div>
        
        <button 
          className="clear-all-btn"
          onClick={clearAllMatches}
          disabled={matches.length === 0}
        >
          🗑️ Clear All Matches
        </button>
      </div>

      {/* Matches List */}
      <div className="matches-section">
        <h2>All Resume Matches ({filteredMatches.length})</h2>
        
        {filteredMatches.length === 0 ? (
          <div className="no-matches">
            <p>No matches found. {matches.length === 0 ? 'Run a bulk analysis first.' : 'Try adjusting your filters.'}</p>
          </div>
        ) : (
          <div className="matches-grid">
            {filteredMatches.map((match) => (
              <div key={match.matchId} className="match-card">
                <div className="match-header">
                  <h3>{match.resumeName}</h3>
                  {getStatusBadge(match.matchStatus)}
                </div>
                
                <div className="match-details">
                  <div className="detail-row">
                    <span className="label">Company:</span>
                    <span className="value">{match.companyName}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Role:</span>
                    <span className="value">{match.roleName}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">JD Index:</span>
                    <span className="value">#{match.jdIndex + 1}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">ATS Score:</span>
                    <span className="value score" style={{ color: getScoreColor(match.atsScore) }}>
                      {match.atsScore}/10
                    </span>
                  </div>
                  <div className="detail-row">
                    <span className="label">File Size:</span>
                    <span className="value">{formatFileSize(match.fileSize)}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">New Name:</span>
                    <span className="value filename">{match.newResumeName}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Match Date:</span>
                    <span className="value">{formatDate(match.matchDate)}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Job Description:</span>
                    <span className="value jd-preview">
                      {match.jobDescription.length > 100 
                        ? match.jobDescription.substring(0, 100) + '...' 
                        : match.jobDescription}
                    </span>
                  </div>
                </div>
                
                <div className="match-footer">
                  <div className="match-id">
                    ID: {match.matchId}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

// Utility functions
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B';
  const exp = Math.floor(Math.log(bytes) / Math.log(1024));
  const pre = 'KMGTPE'.charAt(exp - 1);
  return (bytes / Math.pow(1024, exp)).toFixed(1) + ' ' + pre + 'B';
};

const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  try {
    const date = new Date(dateString);
    return date.toLocaleString();
  } catch (e) {
    return dateString;
  }
};

export default ResumeMatchManager;
