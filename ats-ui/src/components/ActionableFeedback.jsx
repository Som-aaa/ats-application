import React from "react";

const ActionableFeedback = ({ strengths = [], weaknesses = [], suggestions = [] }) => {
  return (
    <div className="space-y-6">
      {/* Strengths */}
      <div>
        <h3 className="text-lg font-semibold text-green-700 mb-3 flex items-center">
          <span className="mr-2">✅</span>
          Strengths
        </h3>
        <ul className="space-y-2">
          {strengths.length > 0 ? (
            strengths.map((strength, index) => (
              <li key={index} className="text-sm text-gray-700 bg-green-50 p-3 rounded-lg border-l-4 border-green-500">
                {strength}
              </li>
            ))
          ) : (
            <li className="text-sm text-gray-500 italic">No strengths identified</li>
          )}
        </ul>
      </div>

      {/* Weaknesses */}
      <div>
        <h3 className="text-lg font-semibold text-red-700 mb-3 flex items-center">
          <span className="mr-2">❌</span>
          Areas for Improvement
        </h3>
        <ul className="space-y-2">
          {weaknesses.length > 0 ? (
            weaknesses.map((weakness, index) => (
              <li key={index} className="text-sm text-gray-700 bg-red-50 p-3 rounded-lg border-l-4 border-red-500">
                {weakness}
              </li>
            ))
          ) : (
            <li className="text-sm text-gray-500 italic">No areas for improvement identified</li>
          )}
        </ul>
      </div>

      {/* Suggestions */}
      <div>
        <h3 className="text-lg font-semibold text-blue-700 mb-3 flex items-center">
          <span className="mr-2">💡</span>
          Recommendations
        </h3>
        <ul className="space-y-2">
          {suggestions.length > 0 ? (
            suggestions.map((suggestion, index) => (
              <li key={index} className="text-sm text-gray-700 bg-blue-50 p-3 rounded-lg border-l-4 border-blue-500">
                {suggestion}
              </li>
            ))
          ) : (
            <li className="text-sm text-gray-500 italic">No recommendations available</li>
          )}
        </ul>
      </div>
    </div>
  );
};

export default ActionableFeedback; 