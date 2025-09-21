import React, { useState, useRef } from 'react';
import './ExcelJDropzone.css';

const ExcelJDropzone = ({ onFileChange, disabled = false }) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

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

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    
    if (disabled) return;
    
    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      handleFileSelect(files[0]);
    }
  };

  const handleFileSelect = (file) => {
    setError(null);
    
    // Validate file type
    const validTypes = [
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // .xlsx
      'application/vnd.ms-excel', // .xls
      'application/octet-stream' // fallback for some systems
    ];
    
    const validExtensions = ['.xlsx', '.xls'];
    const fileExtension = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));
    
    if (!validTypes.includes(file.type) && !validExtensions.includes(fileExtension)) {
      setError('Please select a valid Excel file (.xlsx or .xls)');
      return;
    }
    
    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setError('File size must be less than 5MB');
      return;
    }
    
    setSelectedFile(file);
    onFileChange(file);
  };

  const handleFileInputChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleBrowseClick = () => {
    if (!disabled) {
      fileInputRef.current.click();
    }
  };

  const removeFile = () => {
    setSelectedFile(null);
    setError(null);
    onFileChange(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="excel-j-dropzone">
      <div
        className={`dropzone-area ${isDragOver ? 'drag-over' : ''} ${disabled ? 'disabled' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        {!selectedFile ? (
          <>
            <div className="dropzone-icon">📊</div>
            <h3>Upload Job Descriptions Excel File</h3>
            <p>
              Drag and drop your Excel file here, or{' '}
              <button 
                type="button" 
                className="browse-link" 
                onClick={handleBrowseClick}
                disabled={disabled}
              >
                browse to select
              </button>
            </p>
            <p className="file-format-info">
              <strong>Smart Format:</strong> System automatically detects your columns! 🧠
            </p>
            <div className="file-requirements">
              <p><strong>Requirements:</strong></p>
              <ul>
                <li>Excel format (.xlsx or .xls)</li>
                <li><strong>Smart Column Detection:</strong> System automatically finds:</li>
                <li style={{marginLeft: '20px'}}>• Company information (Company, Organization, Employer, etc.)</li>
                <li style={{marginLeft: '20px'}}>• Job role/title (Role, Title, Position, Job, etc.)</li>
                <li style={{marginLeft: '20px'}}>• Job description (Description, Requirements, Responsibilities, etc.)</li>
                <li>Maximum file size: 5MB</li>
              </ul>
              <div className="format-note">
                <p><strong>💡 Note:</strong> The system intelligently detects your column structure. You can use any header names!</p>
              </div>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,.xls"
              onChange={handleFileInputChange}
              style={{ display: 'none' }}
              disabled={disabled}
            />
          </>
        ) : (
          <div className="file-selected">
            <div className="file-info">
              <div className="file-icon">📄</div>
              <div className="file-details">
                <h4>{selectedFile.name}</h4>
                <p>Size: {(selectedFile.size / 1024 / 1024).toFixed(2)} MB</p>
                <p>Type: {selectedFile.type || 'Excel file'}</p>
              </div>
            </div>
            <button
              type="button"
              className="remove-file-btn"
              onClick={removeFile}
              disabled={disabled}
            >
              ✕ Remove
            </button>
          </div>
        )}
      </div>
      
      {error && (
        <div className="error-message">
          <span className="error-icon">⚠️</span>
          {error}
        </div>
      )}
      
      {selectedFile && (
        <div className="success-message">
          <span className="success-icon">✅</span>
          File selected successfully! Ready to analyze.
        </div>
      )}
    </div>
  );
};

export default ExcelJDropzone;
