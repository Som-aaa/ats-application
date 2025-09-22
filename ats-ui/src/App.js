import React, { useState, useRef } from "react";
import { BrowserRouter as Router, Routes, Route, useNavigate, useLocation } from "react-router-dom";
import ATSResultCard from "./components/ATSResultCard";
import ResumeDropzone from "./components/ResumeDropzone";
import ExcelJDropzone from "./components/ExcelJDropzone";
import ResultsPage from "./pages/ResultsPage";
import Mode2ResultsPage from "./pages/Mode2ResultsPage";
import BulkJDResultsPage from "./pages/BulkJDResultsPage";
import BackButton from "./components/BackButton";
import "./styles/LandingPage.css";
import "./styles/Mode2Report.css";
import "./styles/Mode1Report.css";
import "./styles/ResultsPage.css";

// For icons (using emoji for simplicity, can swap for SVGs)
const featureIcons = [
  "🔑", "📊", "✅", "🤖", "📄", "🔒", "📝", "🏆", "📋", "🔍"
];
const featureLabels = [
  "Keyword Optimization",
  "Skill Gap Analysis",
  "Job Match Score",
  "AI-Powered Insights",
  "PDF & DOCX Support",
  "Privacy Guaranteed",
  "Spelling & Grammar",
  "Quantified Achievements",
  "Section Completeness",
  "Active Voice Detection"
];

function UploadPage({ onResult }) {
  const [mode, setMode] = useState(null); // 1, 2, or 4
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [resumeFile, setResumeFile] = useState(null);
  const [resumeFiles, setResumeFiles] = useState([]);
  const [jdText, setJdText] = useState("");
  const [jdFile, setJdFile] = useState(null);
  const formRef = useRef(null);
  const navigate = useNavigate();

  const handleShowForm = (selectedMode) => {
    setMode(selectedMode);
    setResumeFile(null);
    setResumeFiles([]);
    setJdText("");
    setJdFile(null);
    setError(null);
    setTimeout(() => {
      if (formRef.current) {
        formRef.current.scrollIntoView({ behavior: "smooth" });
      }
    }, 100);
  };

  const handleSubmit = async (e) => {
    if (e && e.preventDefault) e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
      let url = `${API_BASE_URL}/api/mode1`;
      
      if (mode === 1) {
        if (!resumeFile) throw new Error("Resume file is required");
        formData.append("resume", resumeFile);
      } else if (mode === 2) {
        if (!resumeFile) throw new Error("Resume file is required");
        if (!jdText.trim()) throw new Error("Job description is required");
        url = `${API_BASE_URL}/api/mode2`;
        formData.append("resume", resumeFile);
        formData.append("jd", jdText);
      } else if (mode === 4) {
        console.log("Mode 4 - resumeFiles:", resumeFiles);
        console.log("Mode 4 - jdFile:", jdFile);
        if (resumeFiles.length === 0) throw new Error("At least one resume file is required");
        if (!jdFile) throw new Error("Job descriptions Excel file is required");
        url = `${API_BASE_URL}/api/mode4`;
        resumeFiles.forEach((file, index) => {
          console.log("Appending resume file:", file.name, "at index:", index);
          formData.append("resumes", file);
        });
        formData.append("jobDescriptions", jdFile);
      }
      
      console.log("Sending request to:", url);
      console.log("FormData contents:", Array.from(formData.entries()));
      
      const response = await fetch(url, {
        method: "POST",
        body: formData,
        headers: {
          // Don't set Content-Type for FormData, let browser set it with boundary
        }
      });
      
      console.log("Response status:", response.status);
      console.log("Response headers:", response.headers);
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error("Server error response:", errorText);
        throw new Error(`Server error: ${response.status} - ${errorText}`);
      }
      
      const data = await response.json();
      console.log("Response data:", data);
      
      // Pass result to appropriate report page
      if (mode === 4) {
        console.log("Navigating to bulk-jd-report with data:", data);
        navigate("/bulk-jd-report", { state: { mode, data } });
      } else {
        console.log("Navigating to report with data:", data);
        navigate("/report", { state: { mode, data } });
      }
    } catch (err) {
      console.error("Error details:", err);
      setError(`Something went wrong. Please try again. ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <div className="header-content">
          <div className="logo">
            <span className="logo-icon">📋</span>
            <span className="logo-text">sjesora</span>
          </div>
          <nav className="header-nav">
            <a href="#features" className="nav-link">Features</a>
            <a href="#how-it-works" className="nav-link">How it Works</a>
            <a href="#contact" className="nav-link">Contact</a>
          </nav>
        </div>
      </header>

      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-background">
          <div className="hero-overlay"></div>
        </div>
        <div className="hero-content">
          <div className="hero-text">
            <h1 className="hero-title">
              Optimize Your Resume for 
              <span className="highlight"> ATS Success</span>
            </h1>
            <p className="hero-description">
              Get instant feedback on your resume with our advanced AI-powered ATS checker. 
              Optimize keywords, improve formatting, and increase your chances of landing interviews.
            </p>
            <div className="hero-stats">
              <div className="stat">
                <span className="stat-number">95%</span>
                <span className="stat-label">Success Rate</span>
              </div>
              <div className="stat">
                <span className="stat-number">10K+</span>
                <span className="stat-label">Resumes Analyzed</span>
              </div>
              <div className="stat">
                <span className="stat-number">24/7</span>
                <span className="stat-label">Instant Results</span>
              </div>
            </div>
          </div>
          <div className="hero-actions">
            <div className="mode-selector">
              <div className="mode-card" onClick={() => handleShowForm(1)}>
                <div className="mode-icon">📄</div>
                <h3>Resume Analysis</h3>
                <p>Get detailed feedback on your resume structure, keywords, and formatting</p>
                <div className="mode-features">
                  <span>✓ ATS Score</span>
                  <span>✓ Keyword Analysis</span>
                  <span>✓ Format Check</span>
                </div>
              </div>
              <div className="mode-card featured" onClick={() => handleShowForm(2)}>
                <div className="mode-badge">Most Popular</div>
                <div className="mode-icon">🎯</div>
                <h3>Job Match Analysis</h3>
                <p>Compare your resume against specific job descriptions for perfect alignment</p>
                <div className="mode-features">
                  <span>✓ Match Score</span>
                  <span>✓ Skill Gaps</span>
                  <span>✓ Custom Recommendations</span>
                </div>
              </div>
              <div className="mode-card" onClick={() => handleShowForm(4)}>
                <div className="mode-icon">📊</div>
                <h3>Bulk JD Analysis</h3>
                <p>Analyze multiple job descriptions at once for keyword optimization</p>
                <div className="mode-features">
                  <span>✓ Multiple JDs</span>
                  <span>✓ Keyword Consolidation</span>
                  <span>✓ Detailed Report</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="features-section">
        <div className="container">
          <div className="section-header">
            <h2>Why Choose Our ATS Optimizer?</h2>
            <p>Advanced features designed to maximize your resume's impact</p>
          </div>
          <div className="features-grid">
            {featureLabels.map((label, i) => (
              <div className="feature-card" key={label}>
                <div className="feature-icon">{featureIcons[i]}</div>
                <h3>{label}</h3>
                <p>Advanced AI analysis to optimize your resume for maximum ATS compatibility</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section id="how-it-works" className="how-it-works-section">
        <div className="container">
          <div className="section-header">
            <h2>How It Works</h2>
            <p>Simple steps to optimize your resume</p>
          </div>
          <div className="steps-grid">
            <div className="step">
              <div className="step-number">1</div>
              <h3>Upload Resume</h3>
              <p>Upload your resume in PDF or DOCX format</p>
            </div>
            <div className="step">
              <div className="step-number">2</div>
              <h3>AI Analysis</h3>
              <p>Our AI analyzes your resume against ATS standards</p>
            </div>
            <div className="step">
              <div className="step-number">3</div>
              <h3>Get Results</h3>
              <p>Receive detailed feedback and optimization suggestions</p>
            </div>
          </div>
        </div>
      </section>

      {/* Upload Form Section */}
      <section ref={formRef} className="upload-section">
        <div className="container">
          {mode && (
            <div className="upload-container">
              <div className="upload-header">
                <h2>{mode === 1 ? "Resume Analysis" : "Job Match Analysis"}</h2>
                <p>
                  {mode === 1
                    ? "Upload your resume to get comprehensive ATS feedback"
                    : "Upload your resume and job description for targeted analysis"}
                </p>
              </div>
              <div className="upload-form">
                {mode === 1 && (
                  <ResumeDropzone
                    onFileChange={file => setResumeFile(file)}
                    disabled={loading}
                  />
                )}
                {mode === 2 && (
                  <>
                    <ResumeDropzone
                      onFileChange={file => setResumeFile(file)}
                      disabled={loading}
                    />
                    <div className="jd-input">
                      <label>Job Description</label>
                      <textarea 
                        value={jdText} 
                        onChange={e => setJdText(e.target.value)} 
                        placeholder="Paste the job description here..."
                        rows={6}
                        required 
                      />
                    </div>
                  </>
                )}
                {mode === 4 && (
                  <>
                    <div className="upload-section-header">
                      <h3>Step 1: Upload Resumes</h3>
                      <p>Upload multiple resume files (PDF, DOCX) - maximum 20 files</p>
                    </div>
                    <ResumeDropzone
                      onFileChange={files => {
                        console.log("ResumeDropzone onFileChange called with:", files);
                        const fileArray = Array.isArray(files) ? files : [files];
                        console.log("Setting resumeFiles to:", fileArray);
                        setResumeFiles(fileArray);
                      }}
                      disabled={loading}
                      multiple={true}
                    />
                    <div className="upload-section-header">
                      <h3>Step 2: Upload Job Descriptions</h3>
                      <p>Upload Excel file with job descriptions in Column A</p>
                      <a 
                        href="/EXCEL_TEMPLATE_GUIDE.html" 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="template-guide-link"
                      >
                        📋 View Excel Template Guide
                      </a>
                    </div>
                    <ExcelJDropzone
                      onFileChange={file => setJdFile(file)}
                      disabled={loading}
                    />
                  </>
                )}
                <button
                  className="submit-btn"
                  type="button"
                  onClick={handleSubmit}
                  disabled={loading || 
                    (mode === 1 && !resumeFile) || 
                    (mode === 2 && (!resumeFile || !jdText.trim())) || 
                    (mode === 4 && (resumeFiles.length === 0 || !jdFile))}
                >
                  {loading ? (
                    <span className="loading-spinner">
                      <span></span>
                      <span></span>
                      <span></span>
                    </span>
                  ) : (
                    mode === 4 ? "Analyze Resumes & Job Descriptions" : "Analyze Resume"
                  )}
                </button>
                {error && <div className="error-message">{error}</div>}
              </div>
            </div>
          )}
        </div>
      </section>

      {/* Footer */}
      <footer className="app-footer">
        <div className="container">
          <div className="footer-content">
            <div className="footer-section">
              <h3>ATS Optimizer</h3>
              <p>Advanced resume optimization powered by AI</p>
            </div>
            <div className="footer-section">
              <h4>Features</h4>
              <ul>
                <li>Resume Analysis</li>
                <li>Job Match Scoring</li>
                <li>Keyword Optimization</li>
                <li>Format Checking</li>
              </ul>
            </div>
            <div className="footer-section">
              <h4>Support</h4>
              <ul>
                <li>Help Center</li>
                <li>Privacy Policy</li>
                <li>Terms of Service</li>
                <li>Contact Us</li>
              </ul>
            </div>
          </div>
          <div className="footer-bottom">
            <p>&copy; 2024 sjesora. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

function ReportPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { mode, data } = location.state || {};

  if (!mode || !data) {
    // If no data, redirect to landing
    navigate("/");
    return null;
  }

  return (
    <div className="report-container">
      <BackButton onClick={() => navigate("/")} />
      {mode === 1 ? (
        <ResultsPage data={data} mode={mode} />
      ) : (
        <Mode2ResultsPage data={data} mode={mode} />
      )}
    </div>
  );
}

function BulkJDReportPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { mode, data } = location.state || {};

  console.log("BulkJDReportPage - location.state:", location.state);
  console.log("BulkJDReportPage - mode:", mode);
  console.log("BulkJDReportPage - data:", data);

  if (!mode || !data) {
    console.log("BulkJDReportPage - No data, redirecting to landing");
    // If no data, redirect to landing
    navigate("/")
    return null;
  }

  console.log("BulkJDReportPage - Rendering with data");
  return (
    <div className="report-container">
      <BackButton onClick={() => navigate("/")} />
      <BulkJDResultsPage data={data} mode={mode} />
    </div>
  );
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<UploadPage />} />
        <Route path="/report" element={<ReportPage />} />
        <Route path="/bulk-jd-report" element={<BulkJDReportPage />} />
      </Routes>
    </Router>
  );
}

export default App;
