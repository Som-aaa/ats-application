# ATS Optimizer

## Overview

**ATS Optimizer** is a web application that analyzes resumes for ATS (Applicant Tracking System) compatibility and provides job match analysis against specific job descriptions. It leverages OpenAI for AI-powered feedback and is built with a modern Java Spring Boot backend and a React frontend.

---

## Architecture

### 1. Backend (Java Spring Boot)
- **Purpose:** Handles file uploads, interacts with OpenAI, parses AI responses, and serves results via REST API.
- **Key Components:**
  - `ATSController.java`: Main REST API controller for resume analysis endpoints.
  - `HealthController.java`: Provides health and config endpoints for diagnostics.
  - `ATSService.java`: Business logic for parsing resumes, job descriptions, and AI responses.
  - `PromptUtils.java`: Builds prompts for OpenAI API.
  - `utils/`: Helper classes for file handling, API key management, etc.

### 2. Frontend (React)
- **Purpose:** Provides a modern, user-friendly UI for uploading resumes, viewing results, and interacting with the analysis.
- **Key Components:**
  - `App.js`: Main app structure, routing, and page layout.
  - `pages/ResultsPage.jsx` & `pages/Mode2ResultsPage.jsx`: Display analysis results for Mode 1 (resume only) and Mode 2 (resume + job description).
  - `components/`: Reusable UI components (dropzone, scorecards, feedback, etc.).
  - `styles/`: Modern CSS for a professional look and responsive design.

---

## Features

### Modes
- **Mode 1:** Resume-only ATS analysis.
- **Mode 2:** Resume + Job Description match analysis.

### Frontend Features
- Modern, responsive UI with clear navigation and beautiful cards.
- Drag-and-drop resume upload with file validation.
- Real-time feedback and error handling.
- Collapsible, detailed result sections for strengths, weaknesses, skills, projects, certificates, and gaps.
- Friendly messages for empty sections (no more "• . None").
- Copyright and branding.

### Backend Features
- REST API endpoints for both analysis modes.
- Health and config endpoints for diagnostics.
- CORS enabled for development.
- Rate limiting to prevent abuse.
- Robust parsing of OpenAI responses, handling both old and new formats.

---

## Workflow

### 1. User Uploads Resume (and optionally Job Description)
- The user selects Mode 1 or Mode 2.
- The frontend collects the file (and job description for Mode 2) and sends it to the backend via a POST request.

### 2. Backend Processing
- The backend receives the file(s), builds a prompt, and sends it to OpenAI.
- The AI response is parsed for:
  - ATS Score
  - Career Summary
  - Strengths & Weaknesses
  - Suggestions
  - Work Experience, Projects, Certificates, Technical Skills (with matched skills and gaps)
- The backend returns a structured JSON response.

### 3. Frontend Display
- The frontend receives the JSON and displays:
  - Score and summary at the top.
  - Actionable feedback (strengths, weaknesses, suggestions).
  - Collapsible cards for each section (work experience, projects, etc.).
  - Friendly messages for empty sections.
  - Clean, trimmed bullet points for skills and gaps.

---

## Key Improvements & Fixes

- **UI Modernization:** Complete redesign for a professional, modern look.
- **CORS Issues:** Fixed by allowing all origins during development.
- **Health Endpoint Conflict:** Resolved duplicate `/api/health` mapping.
- **Data Structure Handling:** Frontend now supports both old and new backend response formats.
- **Bullet Point Cleanup:** No more "• . None" or "• . SQL" – all items are trimmed and cleaned.
- **Error Handling:** Improved error messages and debugging in both frontend and backend.
- **Testing Tools:** Added a test HTML page for API diagnostics.
- **Branding:** Updated copyright and logo.

---

## API Endpoints

### Main Endpoints
- `POST /api/mode1` – Resume-only analysis
- `POST /api/mode2` – Resume + Job Description analysis

### Diagnostics
- `GET /api/health` – Health check
- `GET /api/config` – Returns backend config
- `GET /api/cache-status` – Cache status
- `POST /api/clear-cache` – Clear backend cache
- `GET /api/rate-limit-status` – Rate limit info

---

## How to Run

### Prerequisites
- Java 17 or higher
- Node.js 16 or higher
- OpenAI API key

### 1. Backend Setup
```bash
# Navigate to the project root
cd ATS

# Set up environment variables (Recommended)
cp .env.example .env
# Edit .env file and add your OpenAI API key:
# OPENAI_API_KEY=sk-your-openai-api-key-here

# Run the Spring Boot application
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

### 🔐 API Key Setup (Important!)

**Option 1: Using .env file (Recommended)**
1. Copy the example file: `cp .env.example .env`
2. Edit `.env` and add your API key: `OPENAI_API_KEY=sk-your-openai-api-key-here`
3. The `.env` file is already in `.gitignore` so it won't be pushed to GitHub

**Option 2: Using environment variable**
```bash
# Windows
set OPENAI_API_KEY=sk-your-openai-api-key-here

# Linux/Mac
export OPENAI_API_KEY=sk-your-openai-api-key-here
```

**Option 3: Using api-key.txt file (Legacy)**
```bash
echo "your-openai-api-key-here" > api-key.txt
```

### 2. Frontend Setup
```bash
# Navigate to frontend directory
cd ats-ui/

# Install dependencies
npm install

# Start the app
npm start
```

Access at [http://localhost:3000](http://localhost:3000)

### 3. Testing
- Use `test-api.html` for direct API testing.

---

## Customization

- **Prompt Tuning:** Edit `PromptUtils.java` to change how the AI is prompted.
- **UI Branding:** Update logo, colors, and footer in `App.js` and CSS files.
- **Section Logic:** Adjust parsing or frontend rendering for new resume/job description fields.

---

## Credits

- **Project Lead & Branding:** sjesora
- **UI/UX & Engineering:** [Your Team/Contributors]
- **AI Integration:** OpenAI

---

## Support

For issues, feature requests, or contributions, please contact the maintainer or open an issue in your project repository. 