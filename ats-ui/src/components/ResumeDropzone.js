import React, { useRef, useState } from "react";
import "../styles/LandingPage.css";

const ResumeDropzone = ({ onFileChange, disabled, multiple = false }) => {
  const inputRef = useRef();
  const [isDragOver, setIsDragOver] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState([]);

  const handleBoxClick = () => {
    if (!disabled) {
      // Clear existing files when clicking to change
      if (selectedFiles.length > 0) {
        setSelectedFiles([]);
        if (multiple) {
          onFileChange([]);
        } else {
          onFileChange(null);
        }
      }
      inputRef.current.click();
    }
  };

  const validateFile = (file) => {
    console.log("Validating file:", file.name, "Type:", file.type, "Size:", file.size);
    const validTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain'];
    const maxSize = 10 * 1024 * 1024; // 10MB

    if (!validTypes.includes(file.type)) {
      console.log("Invalid file type:", file.type);
      alert('Please upload a valid file type (PDF, DOC, DOCX, or TXT)');
      return false;
    }

    if (file.size > maxSize) {
      console.log("File too large:", file.size);
      alert('Maximum file size is 10MB');
      return false;
    }

    console.log("File validation passed:", file.name);
    return true;
  };

  const handleFileChange = (e) => {
    console.log("File change event:", e.target.files);
    if (e.target.files && e.target.files.length > 0) {
      const files = Array.from(e.target.files);
      console.log("Selected files:", files);
      const validFiles = files.filter(validateFile);
      console.log("Valid files after filtering:", validFiles);
      
      if (validFiles.length > 0) {
        // Enforce maximum file limit for multiple mode
        if (multiple && validFiles.length > 20) {
          alert('Maximum 20 files allowed. Only the first 20 files will be selected.');
          validFiles.splice(20);
        }
        
        setSelectedFiles(validFiles);
        console.log("Setting selected files:", validFiles);
        if (multiple) {
          onFileChange(validFiles);
        } else {
          onFileChange(validFiles[0]);
        }
      } else {
        console.log("No valid files found");
      }
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    if (disabled) return;
    console.log("Drop event files:", e.dataTransfer.files);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const files = Array.from(e.dataTransfer.files);
      console.log("Dropped files:", files);
      const validFiles = files.filter(validateFile);
      console.log("Valid dropped files:", validFiles);
      
      if (validFiles.length > 0) {
        // Enforce maximum file limit for multiple mode
        if (multiple && validFiles.length > 20) {
          alert('Maximum 20 files allowed. Only the first 20 files will be selected.');
          validFiles.splice(20);
        }
        
        setSelectedFiles(validFiles);
        console.log("Setting dropped files:", validFiles);
        if (multiple) {
          onFileChange(validFiles);
        } else {
          onFileChange(validFiles[0]);
        }
      } else {
        console.log("No valid dropped files found");
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

  const clearFiles = () => {
    setSelectedFiles([]);
    if (multiple) {
      onFileChange([]);
    } else {
      onFileChange(null);
    }
  };

  const removeFile = (indexToRemove) => {
    if (multiple) {
      const newFiles = selectedFiles.filter((_, index) => index !== indexToRemove);
      setSelectedFiles(newFiles);
      onFileChange(newFiles);
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div
      className={`resume-dropzone ${isDragOver ? 'dragover' : ''} ${selectedFiles.length > 0 ? 'has-file' : ''}`}
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
        ref={inputRef}
        style={{ display: "none" }}
        onChange={handleFileChange}
        disabled={disabled}
        multiple={multiple}
      />
      
      {selectedFiles.length > 0 ? (
        <div className="file-selected">
          {multiple && selectedFiles.length > 1 ? (
            <>
              <div className="file-icon">📁</div>
              <div className="file-info">
                <div className="file-name">{selectedFiles.length} files selected</div>
                <div className="file-list">
                  {selectedFiles.map((file, index) => (
                    <div key={index} className="file-item">
                      <span className="file-item-name">{file.name}</span>
                      <button 
                        type="button"
                        className="file-remove-btn"
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
              <div className="file-change">Click to change files</div>
            </>
          ) : (
            <>
              <div className="file-icon">📄</div>
              <div className="file-info">
                <div className="file-name">{selectedFiles[0].name}</div>
                <div className="file-size">{formatFileSize(selectedFiles[0].size)}</div>
              </div>
              <div className="file-change">Click to change file</div>
            </>
          )}
        </div>
      ) : (
        <>
          <div className="resume-dropzone-icon">
            <span role="img" aria-label="upload">📁</span>
          </div>
          <div className="resume-dropzone-main">
            {isDragOver ? 
              (multiple ? 'Drop your resumes here' : 'Drop your resume here') : 
              (multiple ? 'Click to upload or drag and drop multiple resumes' : 'Click to upload or drag and drop')
            }
          </div>
          <div className="resume-dropzone-info">
            Supported formats: PDF, DOCX, DOC, TXT (Max 10MB)
            {multiple && ' - Select multiple files'}
          </div>
          <div className="resume-dropzone-btn">
            Choose File
          </div>
        </>
      )}
    </div>
  );
};

export default ResumeDropzone; 