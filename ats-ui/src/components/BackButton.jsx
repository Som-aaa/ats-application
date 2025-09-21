import React from "react";

const BackButton = ({ onClick, children = "← Back to Upload" }) => {
  return (
    <button
      onClick={onClick}
      className="back-button"
    >
      <span className="back-icon">←</span>
      {children}
    </button>
  );
};

export default BackButton; 