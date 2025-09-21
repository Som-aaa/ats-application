import React, { useState } from "react";
import ResumeScoreCircle from "../components/ResumeScoreCircle";
import ActionableFeedback from "../components/ActionableFeedback";
import ATSScorecard from "../components/ATSScorecard";
import RecommendationBox from "../components/RecommendationBox";
import CareerOverviewBox from "../components/CareerOverviewBox";

const Mode2ResultsPage = ({ data, mode = 2 }) => {
  const [expandedSections, setExpandedSections] = useState({
    workExperience: false,
    certificates: false,
    projects: false,
    technicalSkills: false
  });

  // Extract data from the API response
  const atsScore = data?.atsScore?.[0] || 8;
  const careerSummary = data?.careerSummary?.[0] || "Experienced professional with strong technical skills.";
  const strengths = data?.strengths || [];
  const weaknesses = data?.weaknesses || [];
  const suggestions = data?.suggestions || [];

  // Extract section data for Mode 2
  const workExperience = data?.workExperience || {};
  const certificates = data?.certificates || {};
  const projects = data?.projects || {};
  const technicalSkills = data?.technicalSkills || {};

  const toggleSection = (section) => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  const renderSectionCard = (title, sectionData, icon) => (
    <div className="bg-white rounded-xl shadow p-6">
      <div 
        className="flex justify-between items-center cursor-pointer"
        onClick={() => toggleSection(title.toLowerCase().replace(/\s+/g, ''))}
      >
        <h3 className="text-lg font-semibold text-gray-800">{icon} {title}</h3>
        <span className="text-gray-500">
          {expandedSections[title.toLowerCase().replace(/\s+/g, '')] ? '▼' : '▶'}
        </span>
      </div>
      {expandedSections[title.toLowerCase().replace(/\s+/g, '')] && (
        <div className="mt-4 space-y-4">
          {/* Matched Skills */}
          <div>
            <h4 className="font-semibold text-green-700 mb-2">✅ Matched Skills</h4>
            <div className="bg-green-50 p-3 rounded-lg">
              {sectionData.matchedSkills && Array.isArray(sectionData.matchedSkills) && sectionData.matchedSkills.length > 0 ? (
                sectionData.matchedSkills.length === 1 && sectionData.matchedSkills[0].trim().replace(/^\.*\s*/, '') === 'None' ? (
                  <p className="text-sm text-gray-500 italic">No matched skills found</p>
                ) : (
                  <ul className="space-y-1">
                    {sectionData.matchedSkills.map((skill, index) => {
                      const cleanSkill = skill.trim().replace(/^\.*\s*/, '');
                      return (
                        <li key={index} className="text-sm text-gray-700">
                          • {cleanSkill}
                        </li>
                      );
                    })}
                  </ul>
                )
              ) : sectionData.matchedSkills && typeof sectionData.matchedSkills === 'string' && sectionData.matchedSkills.trim().replace(/^\.*\s*/, '') !== 'None' ? (
                <p className="text-sm text-gray-700">{sectionData.matchedSkills.trim().replace(/^\.*\s*/, '')}</p>
              ) : (
                <p className="text-sm text-gray-500 italic">No matched skills found</p>
              )}
            </div>
          </div>

          {/* Gaps */}
          <div>
            <h4 className="font-semibold text-red-700 mb-2">❌ Gaps</h4>
            <div className="bg-red-50 p-3 rounded-lg">
              {sectionData.gaps && Array.isArray(sectionData.gaps) && sectionData.gaps.length > 0 ? (
                sectionData.gaps.length === 1 && sectionData.gaps[0].trim().replace(/^\.*\s*/, '') === 'None' ? (
                  <p className="text-sm text-gray-500 italic">No gaps identified</p>
                ) : (
                  <ul className="space-y-1">
                    {sectionData.gaps.map((gap, index) => {
                      const cleanGap = gap.trim().replace(/^\.*\s*/, '');
                      return (
                        <li key={index} className="text-sm text-gray-700">
                          • {cleanGap}
                        </li>
                      );
                    })}
                  </ul>
                )
              ) : sectionData.gaps && typeof sectionData.gaps === 'string' && sectionData.gaps.trim().replace(/^\.*\s*/, '') !== 'None' ? (
                <p className="text-sm text-gray-700">{sectionData.gaps.trim().replace(/^\.*\s*/, '')}</p>
              ) : (
                <p className="text-sm text-gray-500 italic">No gaps identified</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="p-6 max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-800">Job Match Analysis</h1>
          <div className="text-sm text-gray-600 bg-white px-4 py-2 rounded-lg shadow">
            Mode {mode}: Resume + Job Description Match
          </div>
        </div>

        {/* Top Section - Score and Feedback */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          {/* Resume Score + ATS Scorecard */}
          <div className="space-y-6">
            <div className="bg-white rounded-xl shadow p-6 flex flex-col items-center">
              <ResumeScoreCircle score={atsScore} />
              <p className="text-sm text-gray-600 mt-2">Job Match Score</p>
            </div>

            <div className="bg-white rounded-xl shadow p-6">
              <ATSScorecard />
            </div>
          </div>

          {/* Actionable Feedback */}
          <div className="bg-white rounded-xl shadow p-6">
            <h2 className="text-xl font-bold mb-4">Job Match Analysis</h2>
            <ActionableFeedback 
              strengths={strengths}
              weaknesses={weaknesses}
              suggestions={suggestions}
            />
          </div>
        </div>

        {/* Section Analysis */}
        <div className="space-y-6 mb-8">
          {renderSectionCard("Work Experience", workExperience, "💼")}
          {renderSectionCard("Certificates", certificates, "🏆")}
          {renderSectionCard("Projects", projects, "📚")}
          {renderSectionCard("Technical Skills", technicalSkills, "🧰")}
        </div>

        {/* Summary Boxes */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <RecommendationBox 
            text={careerSummary}
            title="Career Summary"
          />
          <CareerOverviewBox 
            text={`Job match analysis completed with a score of ${atsScore}/10. Review the detailed breakdown above to understand your strengths and areas for improvement.`}
            title="Match Overview"
          />
        </div>
      </div>
    </div>
  );
};

export default Mode2ResultsPage; 