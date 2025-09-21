import React from "react";

const ResumeDetailCard = ({ title, description, score, items = [] }) => {
  const getScoreColor = (score) => {
    if (score >= 8) return "#10B981";
    if (score >= 6) return "#F59E0B";
    return "#EF4444";
  };

  const getScoreText = (score) => {
    if (score >= 8) return "Excellent";
    if (score >= 6) return "Good";
    return "Needs Improvement";
  };

  return (
    <div className="bg-white rounded-lg shadow p-4 border-l-4" style={{ borderLeftColor: getScoreColor(score) }}>
      <div className="flex justify-between items-start mb-3">
        <h3 className="font-semibold text-gray-800">{title}</h3>
        <div className="text-right">
          <div className="text-lg font-bold" style={{ color: getScoreColor(score) }}>
            {score}/10
          </div>
          <div className="text-xs text-gray-500">{getScoreText(score)}</div>
        </div>
      </div>
      
      <p className="text-sm text-gray-600 mb-3">{description}</p>
      
      {items.length > 0 && (
        <div className="space-y-1">
          {items.slice(0, 3).map((item, index) => (
            <div key={index} className="text-xs text-gray-700 bg-gray-50 px-2 py-1 rounded">
              • {item}
            </div>
          ))}
          {items.length > 3 && (
            <div className="text-xs text-gray-500 italic">
              +{items.length - 3} more items
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ResumeDetailCard; 