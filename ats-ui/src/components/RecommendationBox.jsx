import React from "react";

const RecommendationBox = ({ text, title = "Career Recommendation" }) => {
  return (
    <div className="bg-gradient-to-br from-blue-50 to-indigo-100 rounded-xl p-6 mb-6 border border-blue-200">
      <div className="flex items-center mb-3">
        <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center mr-3">
          <span className="text-white text-sm font-bold">💼</span>
        </div>
        <h3 className="text-lg font-semibold text-blue-800">{title}</h3>
      </div>
      <p className="text-gray-700 leading-relaxed">{text}</p>
      <div className="mt-4 flex items-center text-sm text-blue-600">
        <span className="mr-2">✨</span>
        <span>AI-powered insights</span>
      </div>
    </div>
  );
};

export default RecommendationBox; 