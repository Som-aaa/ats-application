import React, { useState } from 'react';
import './FileRenamerIntegration.css';

/**
 * FileRenamerIntegration - React component for integrating with the file renamer service
 */
const FileRenamerIntegration = () => {
    const [selectedFile, setSelectedFile] = useState(null);
    const [companyName, setCompanyName] = useState('');
    const [roleName, setRoleName] = useState('');
    const [userName, setUserName] = useState('');
    const [isProcessing, setIsProcessing] = useState(false);
    const [status, setStatus] = useState({ message: '', type: '' });

    const handleFileSelect = (event) => {
        const file = event.target.files[0];
        if (file) {
            setSelectedFile(file);
            setStatus({ message: `File selected: ${file.name}`, type: 'info' });
        }
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        
        if (!selectedFile || !companyName.trim() || !roleName.trim()) {
            setStatus({ message: 'Please select a file and fill in company and role names.', type: 'error' });
            return;
        }

        setIsProcessing(true);
        setStatus({ message: 'Processing file...', type: 'info' });

        try {
            // Create FormData for the request
            const formData = new FormData();
            formData.append('file', selectedFile);
            formData.append('companyName', companyName.trim());
            formData.append('roleName', roleName.trim());
            if (userName.trim()) {
                formData.append('userName', userName.trim());
            }

            // Process the file
            const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
            const processResponse = await fetch(`${API_BASE_URL}/api/file-renamer/process`, {
                method: 'POST',
                body: formData
            });

            if (!processResponse.ok) {
                const errorData = await processResponse.json();
                throw new Error(errorData.error || 'Failed to process file');
            }

            const result = await processResponse.json();
            
            if (result.success) {
                setStatus({ message: 'File processed successfully! Downloading...', type: 'success' });
                
                // Download the renamed file
                await downloadRenamedFile(formData);
                
            } else {
                throw new Error(result.error || 'Processing failed');
            }

        } catch (error) {
            setStatus({ message: `Error: ${error.message}`, type: 'error' });
        } finally {
            setIsProcessing(false);
        }
    };

    const downloadRenamedFile = async (formData) => {
        try {
            console.log('DEBUG - React: Starting FileSystemResource download...');
            
            const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
            const response = await fetch(`${API_BASE_URL}/api/file-renamer/download`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error('FileSystemResource download failed');
            }

            // Get filename from response headers
            const contentDisposition = response.headers.get('content-disposition');
            let filename = generatePreviewName();
            
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename\*=UTF-8''(.+)/);
                if (filenameMatch) {
                    filename = decodeURIComponent(filenameMatch[1]);
                } else {
                    const simpleMatch = contentDisposition.match(/filename="(.+)"/);
                    if (simpleMatch) {
                        filename = simpleMatch[1];
                    }
                }
            }

            // Create blob and download
            const blob = await response.blob();
            const downloadUrl = URL.createObjectURL(blob);

            const downloadLink = document.createElement('a');
            downloadLink.href = downloadUrl;
            downloadLink.download = filename;
            document.body.appendChild(downloadLink);
            downloadLink.click();
            document.body.removeChild(downloadLink);

            // Clean up
            URL.revokeObjectURL(downloadUrl);

            console.log('DEBUG - React: FileSystemResource download completed successfully');
            setStatus({ type: 'success', message: '✅ File downloaded successfully using FileSystemResource!' });

        } catch (error) {
            console.error('DEBUG - React: FileSystemResource download failed:', error);
            setStatus({ type: 'error', message: `❌ FileSystemResource download failed: ${error.message}` });
        }
    };

    const clearForm = () => {
        setSelectedFile(null);
        setCompanyName('');
        setRoleName('');
        setUserName('');
        setStatus({ message: '', type: '' });
    };

    const generatePreviewName = () => {
        if (!companyName.trim() && !roleName.trim()) return '';
        
        let preview = '';
        if (companyName.trim()) preview += companyName.trim();
        if (roleName.trim()) preview += (preview ? '_' : '') + roleName.trim();
        if (userName.trim()) preview += (preview ? '_' : '') + userName.trim();
        
        if (preview && selectedFile) {
            const extension = selectedFile.name.substring(selectedFile.name.lastIndexOf('.'));
            preview += extension;
        }
        
        return preview;
    };

    return (
        <div className="file-renamer-integration">
            <div className="header">
                <h2>📁 File Renamer</h2>
                <p>Rename files while preserving content and format</p>
            </div>

            <div className="file-upload-section">
                <label htmlFor="fileInput" className="file-upload-label">
                    <div className="file-upload-area">
                        <span className="upload-icon">📄</span>
                        <span className="upload-text">
                            {selectedFile ? `Selected: ${selectedFile.name}` : 'Click to select a file'}
                        </span>
                    </div>
                </label>
                <input
                    id="fileInput"
                    type="file"
                    onChange={handleFileSelect}
                    accept="*/*"
                    style={{ display: 'none' }}
                />
            </div>

            {selectedFile && (
                <div className="file-info">
                    <h3>File Information</h3>
                    <p><strong>Name:</strong> {selectedFile.name}</p>
                    <p><strong>Size:</strong> {(selectedFile.size / 1024).toFixed(2)} KB</p>
                    <p><strong>Type:</strong> {selectedFile.type || 'Unknown'}</p>
                </div>
            )}

            <form onSubmit={handleSubmit} className="rename-form">
                <div className="form-group">
                    <label htmlFor="companyName">Company Name *</label>
                    <input
                        type="text"
                        id="companyName"
                        value={companyName}
                        onChange={(e) => setCompanyName(e.target.value)}
                        placeholder="Enter company name"
                        required
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="roleName">Role/Position *</label>
                    <input
                        type="text"
                        id="roleName"
                        value={roleName}
                        onChange={(e) => setRoleName(e.target.value)}
                        placeholder="Enter role or position"
                        required
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="userName">User Name (Optional)</label>
                    <input
                        type="text"
                        id="userName"
                        value={userName}
                        onChange={(e) => setUserName(e.target.value)}
                        placeholder="Enter user name"
                    />
                </div>

                <div className="form-group">
                    <label>Preview New Name:</label>
                    <input
                        type="text"
                        value={generatePreviewName()}
                        readOnly
                        className="preview-input"
                    />
                    <small>This will be the new filename</small>
                </div>

                <div className="form-actions">
                    <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={isProcessing || !selectedFile || !companyName.trim() || !roleName.trim()}
                    >
                        {isProcessing ? '⏳ Processing...' : '💾 Process & Download'}
                    </button>
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={clearForm}
                        disabled={isProcessing}
                    >
                        🗑️ Clear
                    </button>
                </div>
            </form>

            {status.message && (
                <div className={`status-message ${status.type}`}>
                    {status.message}
                </div>
            )}

            <div className="info-section">
                <h3>How it works:</h3>
                <ul>
                    <li>Select a file (PDF, DOC, DOCX, etc.)</li>
                    <li>Enter company name and role/position</li>
                    <li>Optionally add user name</li>
                    <li>The system will generate a new filename: Company_Role_User.ext</li>
                    <li>Download the renamed file with preserved content</li>
                </ul>
            </div>
        </div>
    );
};

export default FileRenamerIntegration;
