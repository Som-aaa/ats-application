import React from "react";

const ATSScorecard = () => {
  const scoreCategories = [
    { name: "Keyword Match", score: 85, color: "#10B981" },
    { name: "Format Compliance", score: 92, color: "#10B981" },
    { name: "Content Quality", score: 78, color: "#F59E0B" },
    { name: "Section Completeness", score: 88, color: "#10B981" },
    { name: "Grammar & Spelling", score: 95, color: "#10B981" }
  ];

  const getScoreColor = (score) => {
    if (score >= 80) return "#10B981";
    if (score >= 60) return "#F59E0B";
    return "#EF4444";
  };

  return (
    <div>
      <h3 className="text-lg font-semibold mb-4">ATS Score Breakdown</h3>
      <div className="space-y-3">
        {scoreCategories.map((category, index) => (
          <div key={index} className="flex items-center justify-between">
            <span className="text-sm text-gray-600">{category.name}</span>
            <div className="flex items-center space-x-2">
              <div className="w-20 bg-gray-200 rounded-full h-2">
                <div
                  className="h-2 rounded-full transition-all duration-300"
                  style={{
                    width: `${category.score}%`,
                    backgroundColor: getScoreColor(category.score)
                  }}
                />
              </div>
              <span className="text-sm font-medium" style={{ color: getScoreColor(category.score) }}>
                {category.score}%
              </span>
            </div>
          </div>
        ))}
      </div>
      
      <div className="mt-4 p-3 bg-blue-50 rounded-lg">
        <p className="text-xs text-blue-700">
          💡 <strong>Tip:</strong> Scores above 80% indicate strong ATS compatibility. 
          Focus on improving areas below 80% for better results.
        </p>
      </div>
    </div>
  );
};

export default ATSScorecard; 