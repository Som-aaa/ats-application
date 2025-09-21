import React from "react";
import "../styles/ATSResultCard.css";

const ATSResultCard = ({ mode, data }) => {
  return (
    <div className="ats-card">
      <h2 className="ats-title">{mode === 1 ? "Career Summary" : "Match Score"}</h2>
      {mode === 1 ? (
        <>
          <div className="ats-section">
            <strong>Career Summary:</strong>
            <p>{data.careerSummary}</p>
          </div>
          <div className="ats-score ats-score-high">ATS Score: {data.atsScore}/10</div>
          <div className="ats-section ats-strengths">
            <strong>Strengths:</strong>
            <ul>
              {data.strengths.map((s, i) => <li key={i}>{s}</li>)}
            </ul>
          </div>
          <div className="ats-section ats-weaknesses">
            <strong>Weaknesses:</strong>
            <ul>
              {data.weaknesses.map((w, i) => <li key={i}>{w}</li>)}
            </ul>
          </div>
          <div className="ats-section ats-suggestions">
            <strong>Suggestions to Improve:</strong>
            <ul>
              {data.suggestions.map((s, i) => <li key={i}>{s}</li>)}
            </ul>
          </div>
        </>
      ) : (
        <>
          <div className="ats-score ats-score-low">Match Score: {data.matchScore}/10</div>
          <div className="ats-section ats-matched-skills">
            <strong>Matched Skills:</strong>
            <ul>
              {data.matchedSkills.length ? data.matchedSkills.map((s, i) => <li key={i}>{s}</li>) : <li>None</li>}
            </ul>
          </div>
          <div className="ats-section ats-gaps">
            <strong>Gaps:</strong>
            <ul>
              {data.gaps.map((g, i) => <li key={i}>{g}</li>)}
            </ul>
          </div>
          <div className="ats-section ats-suggestions">
            <strong>Suggestions to Improve:</strong>
            <ul>
              {data.suggestions.map((s, i) => <li key={i}>{s}</li>)}
            </ul>
          </div>
        </>
      )}
    </div>
  );
};

export default ATSResultCard; 