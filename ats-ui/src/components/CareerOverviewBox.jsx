import React from "react";

const CareerOverviewBox = ({ text, title = "Career Overview" }) => {
  return (
    <div className="bg-gradient-to-br from-green-50 to-emerald-100 rounded-xl p-6 border border-green-200">
      <div className="flex items-center mb-3">
        <div className="w-8 h-8 bg-green-500 rounded-full flex items-center justify-center mr-3">
          <span className="text-white text-sm font-bold">📈</span>
        </div>
        <h3 className="text-lg font-semibold text-green-800">{title}</h3>
      </div>
      <p className="text-gray-700 leading-relaxed">{text}</p>
      <div className="mt-4 flex items-center text-sm text-green-600">
        <span className="mr-2">🎯</span>
        <span>Professional summary</span>
      </div>
    </div>
  );
};

export default CareerOverviewBox; 