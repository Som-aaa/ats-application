import React from "react";

const ResumeScoreCircle = ({ score = 8 }) => {
  const percentage = (score / 10) * 100;
  const circumference = 2 * Math.PI * 45; // radius = 45
  const strokeDasharray = circumference;
  const strokeDashoffset = circumference - (percentage / 100) * circumference;

  const getScoreColor = (score) => {
    if (score >= 8) return "#10B981"; // green
    if (score >= 6) return "#F59E0B"; // yellow
    return "#EF4444"; // red
  };

  return (
    <div className="relative w-32 h-32">
      <svg className="w-32 h-32 transform -rotate-90" viewBox="0 0 100 100">
        {/* Background circle */}
        <circle
          cx="50"
          cy="50"
          r="45"
          stroke="#E5E7EB"
          strokeWidth="8"
          fill="none"
        />
        {/* Progress circle */}
        <circle
          cx="50"
          cy="50"
          r="45"
          stroke={getScoreColor(score)}
          strokeWidth="8"
          fill="none"
          strokeDasharray={strokeDasharray}
          strokeDashoffset={strokeDashoffset}
          strokeLinecap="round"
          style={{ transition: "stroke-dashoffset 0.5s ease-in-out" }}
        />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="text-center">
          <div className="text-3xl font-bold" style={{ color: getScoreColor(score) }}>
            {score}
          </div>
          <div className="text-sm text-gray-600">/10</div>
        </div>
      </div>
    </div>
  );
};

export default ResumeScoreCircle; 