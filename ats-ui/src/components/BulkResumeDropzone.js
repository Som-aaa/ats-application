import React, { useRef, useState } from "react";
import "../styles/LandingPage.css";

const BulkResumeDropzone = ({ onFilesChange, disabled }) => {
  const inputRef = useRef();
  const [isDragOver, setIsDragOver] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState([]);

  const handleBoxClick = () => {
    if (!disabled) inputRef.current.click();
  };

  const validateFiles = (files) => {
    const validTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain'];
    const maxSize = 10 * 1024 * 1024; // 10MB per file

    for (let file of files) {
      if (!validTypes.includes(file.type)) {
        alert(`Invalid file type: ${file.name}. Please upload PDF, DOC, DOCX, or TXT files only.`);
        return false;
      }

      if (file.size > maxSize) {
        alert(`File ${file.name} is too large. Maximum size is 10MB.`);
        return false;
      }

      // Check for duplicate files
      const isDuplicate = selectedFiles.some(existingFile => 
        existingFile.name === file.name && existingFile.size === file.size
      );
      if (isDuplicate) {
        alert(`File ${file.name} is already selected.`);
        return false;
      }
    }

    return true;
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      const newFiles = Array.from(e.target.files);
      if (validateFiles(newFiles)) {
        const updatedFiles = [...selectedFiles, ...newFiles];
        setSelectedFiles(updatedFiles);
        onFilesChange(updatedFiles);
      }
    }
    // Reset input value to allow selecting the same file again
    e.target.value = '';
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    if (disabled) return;
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const newFiles = Array.from(e.dataTransfer.files);
      if (validateFiles(newFiles)) {
        const updatedFiles = [...selectedFiles, ...newFiles];
        setSelectedFiles(updatedFiles);
        onFilesChange(updatedFiles);
      }
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    if (!disabled) {
      setIsDragOver(true);
    }
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const removeFile = (index) => {
    const newFiles = selectedFiles.filter((_, i) => i !== index);
    setSelectedFiles(newFiles);
    onFilesChange(newFiles);
  };

  const clearAllFiles = () => {
    setSelectedFiles([]);
    onFilesChange([]);
  };

  return (
    <div
      className={`bulk-resume-dropzone ${isDragOver ? 'dragover' : ''} ${selectedFiles.length > 0 ? 'has-files' : ''}`}
      onClick={handleBoxClick}
      onDrop={handleDrop}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      style={{ 
        cursor: disabled ? "not-allowed" : "pointer", 
        opacity: disabled ? 0.6 : 1 
      }}
    >
      <input
        type="file"
        accept=".pdf,.doc,.docx,.txt"
        multiple
        ref={inputRef}
        style={{ display: "none" }}
        onChange={handleFileChange}
        disabled={disabled}
      />
      
      {selectedFiles.length > 0 ? (
        <div className="files-selected">
          <div className="files-header">
            <div className="files-icon">📁</div>
            <div className="files-info">
              <div className="files-count">
                {selectedFiles.length} resume{selectedFiles.length !== 1 ? 's' : ''} selected
              </div>
              <div className="files-total-size">
                Total: {formatFileSize(selectedFiles.reduce((acc, file) => acc + file.size, 0))}
              </div>
            </div>
            <div className="files-actions">
              <button 
                className="files-add-more"
                onClick={(e) => {
                  e.stopPropagation();
                  inputRef.current.click();
                }}
              >
                Add More
              </button>
              <button 
                className="files-clear-all"
                onClick={(e) => {
                  e.stopPropagation();
                  clearAllFiles();
                }}
              >
                Clear All
              </button>
            </div>
          </div>
          
          <div className="files-list">
            {selectedFiles.map((file, index) => (
              <div key={index} className="file-item">
                <div className="file-details">
                  <div className="file-name">{file.name}</div>
                  <div className="file-size">{formatFileSize(file.size)}</div>
                </div>
                <button 
                  className="file-remove"
                  onClick={(e) => {
                    e.stopPropagation();
                    removeFile(index);
                  }}
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <>
          <div className="bulk-resume-dropzone-icon">
            <span role="img" aria-label="upload">📁</span>
          </div>
          <div className="bulk-resume-dropzone-main">
            {isDragOver ? 'Drop your resumes here' : 'Click to upload or drag and drop multiple resumes'}
          </div>
          <div className="bulk-resume-dropzone-info">
            Supported formats: PDF, DOCX, DOC, TXT (Max 10MB each, unlimited files)
          </div>
          <div className="bulk-resume-dropzone-btn">
            Choose Files
          </div>
        </>
      )}
    </div>
  );
};

export default BulkResumeDropzone;
