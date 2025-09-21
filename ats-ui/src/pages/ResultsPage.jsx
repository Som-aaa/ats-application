import React, { useState } from "react";
import ResumeScoreCircle from "../components/ResumeScoreCircle";
import ActionableFeedback from "../components/ActionableFeedback";
import ATSScorecard from "../components/ATSScorecard";
import ResumeDetailCard from "../components/ResumeDetailCard";
import RecommendationBox from "../components/RecommendationBox";
import CareerOverviewBox from "../components/CareerOverviewBox";

const ResultsPage = ({ data, mode = 1 }) => {
  const [expandedSections, setExpandedSections] = useState({
    workExperience: false,
    projects: false,
    certificates: false,
    technicalSkills: false
  });

  // Debug: Log the received data
  console.log('ResultsPage received data:', data);

  // Extract data from the API response
  const atsScore = data?.atsScore?.[0] || 8;
  const careerSummary = data?.careerSummary?.[0] || "Experienced professional with strong technical skills.";
  const strengths = data?.strengths || [];
  const weaknesses = data?.weaknesses || [];
  const suggestions = data?.suggestions || [];
  
  // Extract section data - handle both old and new formats
  const technicalSkills = data?.technicalSkills?.matchedSkills || data?.technicalSkills || [];
  const workExperience = data?.workExperience?.matchedSkills || data?.workExperience || [];
  const projects = data?.projects?.matchedSkills || data?.projects || [];
  const certificates = data?.certificates?.matchedSkills || data?.certificates || [];

  // Debug: Log extracted data
  console.log('Extracted data:', {
    technicalSkills,
    workExperience,
    projects,
    certificates
  });

  // Calculate section scores based on data quality
  const getSectionScore = (items, maxScore = 10) => {
    if (!items || items.length === 0) return 3;
    if (items.length >= 5) return maxScore;
    return Math.min(maxScore, 3 + (items.length * 1.5));
  };

  const technicalScore = getSectionScore(technicalSkills);
  const educationScore = getSectionScore(certificates);
  const workScore = getSectionScore(workExperience);
  const projectScore = getSectionScore(projects);

  const toggleSection = (section) => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  const renderWorkExperience = () => (
    <div className="section-card">
      <div 
        className="section-header"
        onClick={() => toggleSection('workExperience')}
      >
        <h3 className="section-title">💼 Work Experience</h3>
        <span className={`section-toggle ${expandedSections.workExperience ? 'expanded' : ''}`}>
          ▼
        </span>
      </div>
      {expandedSections.workExperience && (
        <div className="section-content">
          {workExperience.length > 0 ? (
            workExperience.map((job, index) => (
              <div key={index} className="item-card work">
                <div className="item-title">
                  {typeof job === 'string' ? job : job.title || 'Work Experience'}
                </div>
                {typeof job === 'object' && job.company && (
                  <div className="item-subtitle">{job.company}</div>
                )}
                {typeof job === 'object' && job.dates && (
                  <div className="item-subtitle">{job.dates}</div>
                )}
                {typeof job === 'object' && job.description && (
                  <div className="item-description">{job.description}</div>
                )}
              </div>
            ))
          ) : (
            <div className="text-gray-500 italic">No work experience found</div>
          )}
        </div>
      )}
    </div>
  );

  const renderProjects = () => (
    <div className="section-card">
      <div 
        className="section-header"
        onClick={() => toggleSection('projects')}
      >
        <h3 className="section-title">📚 Projects</h3>
        <span className={`section-toggle ${expandedSections.projects ? 'expanded' : ''}`}>
          ▼
        </span>
      </div>
      {expandedSections.projects && (
        <div className="section-content">
          {projects.length > 0 ? (
            projects.map((project, index) => (
              <div key={index} className="item-card project">
                <div className="item-title">
                  {typeof project === 'string' ? project : project.title || 'Project'}
                </div>
                {typeof project === 'object' && project.technologies && (
                  <div className="item-subtitle">Technologies: {project.technologies}</div>
                )}
                {typeof project === 'object' && project.description && (
                  <div className="item-description">{project.description}</div>
                )}
              </div>
            ))
          ) : (
            <div className="text-gray-500 italic">No projects found</div>
          )}
        </div>
      )}
    </div>
  );

  const renderCertificates = () => (
    <div className="section-card">
      <div 
        className="section-header"
        onClick={() => toggleSection('certificates')}
      >
        <h3 className="section-title">🏆 Certificates</h3>
        <span className={`section-toggle ${expandedSections.certificates ? 'expanded' : ''}`}>
          ▼
        </span>
      </div>
      {expandedSections.certificates && (
        <div className="section-content">
          {certificates.length > 0 ? (
            certificates.map((cert, index) => (
              <div key={index} className="item-card certificate">
                <div className="item-title">
                  {typeof cert === 'string' ? cert : cert.name || 'Certificate'}
                </div>
                {typeof cert === 'object' && cert.issuer && (
                  <div className="item-subtitle">Issuer: {cert.issuer}</div>
                )}
                {typeof cert === 'object' && cert.date && (
                  <div className="item-subtitle">Date: {cert.date}</div>
                )}
              </div>
            ))
          ) : (
            <div className="text-gray-500 italic">No certificates found</div>
          )}
        </div>
      )}
    </div>
  );

  const renderTechnicalSkills = () => (
    <div className="section-card">
      <div 
        className="section-header"
        onClick={() => toggleSection('technicalSkills')}
      >
        <h3 className="section-title">🧰 Technical Skills</h3>
        <span className={`section-toggle ${expandedSections.technicalSkills ? 'expanded' : ''}`}>
          ▼
        </span>
      </div>
      {expandedSections.technicalSkills && (
        <div className="section-content">
          {technicalSkills.length > 0 ? (
            <div className="skill-tags">
              {technicalSkills.map((skill, index) => (
                <span key={index} className="skill-tag">
                  {typeof skill === 'string' ? skill : skill.name || skill}
                </span>
              ))}
            </div>
          ) : (
            <div className="text-gray-500 italic">No technical skills found</div>
          )}
        </div>
      )}
    </div>
  );

  return (
    <div className="results-container">
      <div className="container">
        {/* Header */}
        <div className="results-header">
          <h1 className="results-title">ATS Analysis Results</h1>
          <p className="results-subtitle">
            {mode === 1 
              ? "Comprehensive analysis of your resume for ATS optimization"
              : "Detailed comparison of your resume against the job description"
            }
          </p>
          <div className="mode-badge">
            Mode {mode}: {mode === 1 ? "Resume Analysis" : "Job Match Analysis"}
          </div>
        </div>

        {/* Score Section */}
        <div className="score-section">
          <ResumeScoreCircle score={atsScore} />
          <p className="score-label">
            {mode === 1 ? "ATS Compatibility Score" : "Job Match Score"}
          </p>
        </div>

        {/* Feedback Section */}
        <div className="feedback-section">
          <h2 className="feedback-title">📊 Analysis Results</h2>
          <div className="feedback-grid">
            <div className="feedback-card strengths">
              <h3 className="feedback-card-title">✅ Strengths</h3>
              <ul className="feedback-list">
                {strengths.length > 0 ? (
                  strengths.map((strength, index) => (
                    <li key={index}>{strength}</li>
                  ))
                ) : (
                  <li>No specific strengths identified</li>
                )}
              </ul>
            </div>
            
            <div className="feedback-card weaknesses">
              <h3 className="feedback-card-title">❌ Areas for Improvement</h3>
              <ul className="feedback-list">
                {weaknesses.length > 0 ? (
                  weaknesses.map((weakness, index) => (
                    <li key={index}>{weakness}</li>
                  ))
                ) : (
                  <li>No specific weaknesses identified</li>
                )}
              </ul>
            </div>
            
            <div className="feedback-card suggestions">
              <h3 className="feedback-card-title">💡 Recommendations</h3>
              <ul className="feedback-list">
                {suggestions.length > 0 ? (
                  suggestions.map((suggestion, index) => (
                    <li key={index}>{suggestion}</li>
                  ))
                ) : (
                  <li>Consider adding more details to your resume</li>
                )}
              </ul>
            </div>
          </div>
        </div>

        {/* Detailed Sections */}
        <div className="section-card">
          <h2 className="section-title">📋 Resume Details</h2>
          {renderWorkExperience()}
          {renderProjects()}
          {renderCertificates()}
          {renderTechnicalSkills()}
        </div>

        {/* Summary Section */}
        <div className="summary-grid">
          <div className="summary-box">
            <h3 className="summary-title">📝 Career Summary</h3>
            <p className="summary-text">{careerSummary}</p>
          </div>
          
          <div className="summary-box">
            <h3 className="summary-title">📈 Analysis Overview</h3>
            <p className="summary-text">
              Your resume has been analyzed with a focus on ATS compatibility and keyword optimization. 
              The score of {atsScore}/10 indicates {atsScore >= 7 ? 'strong' : atsScore >= 5 ? 'moderate' : 'room for improvement'} 
              alignment with ATS systems.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResultsPage; 