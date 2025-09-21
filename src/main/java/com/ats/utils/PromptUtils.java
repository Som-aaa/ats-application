package com.ats.utils;

public class PromptUtils {
    public static String buildMode1Prompt(String resumeText) {
        return "Analyze this resume and extract all relevant information. Format your response exactly as follows:\n\n" +
               "1. Career Summary\n" +
               "[Write a brief summary of the candidate's background]\n\n" +
               "2. ATS Score\n" +
               "Score: [number between 0-10]\n\n" +
               "3. Strengths and Weaknesses\n" +
               "Strengths: [List key strengths found in the resume]\n" +
               "Weaknesses: [List areas for improvement]\n\n" +
               "4. Suggestions to improve\n" +
               "[Provide improvement recommendations]\n\n" +
               "A. Work Experience\n" +
               "Matched Skills: [List actual work experience, job titles, companies, or write 'None' if no work experience found]\n\n" +
               "B. Certificates\n" +
               "Matched Skills: [List actual certificates, certifications, or write 'None' if no certificates found]\n\n" +
               "C. Projects\n" +
               "Matched Skills: [List actual projects, academic projects, or write 'None' if no projects found]\n\n" +
               "D. Technical Skills\n" +
               "Matched Skills: [List actual technical skills, programming languages, tools, or write 'None' if no technical skills found]\n\n" +
               "IMPORTANT: Look carefully through the resume text and extract real information. Only write 'None' if you cannot find any relevant information in that category.\n\n" +
               "Resume:\n" + resumeText;
    }
    
    public static String buildMode1WithJDPrompt(String resumeText, String jdText) {
        return "You are an ATS evaluator. Analyze the following resume against the job description and provide a structured response in exactly this format:\n\n" +
               "1. Career Summary\n" +
               "Provide a concise summary of the candidate's background and experience.\n\n" +
               "2. ATS Score out of 10\n" +
               "Provide a single number between 0 and 10 representing the overall job match score.\n" +
               "Format: 'Score: X' where X is the number.\n\n" +
               "3. Job Details\n" +
               "Company: [Extract the company name from the job description. If no company is mentioned, write 'Unknown Company']\n" +
               "Role: [Extract the job title/role from the job description. If no role is mentioned, write 'Unknown Role']\n" +
               "Match Status: [Based on the ATS score above - write 'MATCHED' if score >= 6, otherwise write 'UNMATCHED']\n\n" +
               "4. Strengths and Weaknesses\n" +
               "Strengths: [List the candidate's strengths relevant to THIS SPECIFIC JOB]\n" +
               "Weaknesses: [List the candidate's weaknesses or gaps for THIS SPECIFIC JOB]\n\n" +
               "5. Suggestions to improve\n" +
               "Provide specific recommendations to improve match for THIS JOB.\n\n" +
               "A. Work Experience\n" +
               "Matched Skills: [List skills from work experience that match THIS JOB'S requirements]\n" +
               "Gaps: [List missing work experience requirements for THIS JOB]\n\n" +
               "B. Certificates\n" +
               "Matched Skills: [List relevant certificates that match THIS JOB'S requirements]\n" +
               "Gaps: [List missing certificate requirements for THIS JOB]\n\n" +
               "C. Projects\n" +
               "Matched Skills: [List relevant project skills that match THIS JOB'S requirements]\n" +
               "Gaps: [List missing project requirements for THIS JOB]\n\n" +
               "D. Technical Skills\n" +
               "Matched Skills: [List technical skills that match THIS JOB'S requirements]\n" +
               "Gaps: [List missing technical skills for THIS JOB]\n\n" +
               "IMPORTANT FORMATTING RULES:\n" +
               "1. Use square brackets [ ] around lists of items\n" +
               "2. Separate multiple items with commas within the brackets\n" +
               "3. If no skills match, write 'None' inside the brackets: [None]\n" +
               "4. If no gaps, write 'None' inside the brackets: [None]\n" +
               "5. Be specific and detailed in your analysis\n" +
               "6. Focus on skills and experience that directly relate to the job requirements\n" +
               "7. For Company and Role, extract the most relevant information from the job description\n" +
               "8. If company name is not clear, use 'Unknown Company'\n" +
               "9. If role is not clear, use 'Unknown Role'\n" +
               "10. IMPORTANT: Calculate Match Status based on the ATS score you provided above\n\n" +
               "EXAMPLE FORMAT:\n" +
               "2. ATS Score out of 10\n" +
               "Score: 8\n\n" +
               "3. Job Details\n" +
               "Company: [Google Inc]\n" +
               "Role: [Senior Software Engineer]\n" +
               "Match Status: [MATCHED]\n\n" +
               "A. Work Experience\n" +
               "Matched Skills: [Java development, Spring Framework, REST APIs, Database design]\n" +
               "Gaps: [No experience with microservices, No cloud platform experience]\n\n" +
               "CRITICAL: You MUST include the '3. Job Details' section with Company and Role extracted from the job description. Do not skip this section.\n\n" +
               "Resume:\n" + resumeText + "\n\nJob Description:\n" + jdText;
    }

    public static String buildMode2Prompt(String resumeText, String jdText) {
        return "You are an ATS evaluator. Analyze the following resume against the job description and provide a structured response in exactly this format:\n\n" +
               "1. Career Summary\n" +
               "Provide a concise summary of the candidate's background and experience.\n\n" +
               "2. ATS Score out of 10\n" +
               "Provide a single number between 0 and 10 representing the overall job match score.\n" +
               "Format: 'Score: X' where X is the number.\n\n" +
               "3. Job Details\n" +
               "Company: [Extract the company name from the job description]\n" +
               "Role: [Extract the job title/role from the job description]\n" +
               "Match Status: [Based on the ATS score above - write 'MATCHED' if score >= 6, otherwise write 'UNMATCHED']\n\n" +
               "4. Strengths and Weaknesses\n" +
               "Strengths: [List the candidate's strengths relevant to THIS SPECIFIC JOB]\n" +
               "Weaknesses: [List the candidate's weaknesses or gaps for THIS SPECIFIC JOB]\n\n" +
               "5. Suggestions to improve\n" +
               "Provide specific recommendations to improve match for THIS JOB.\n\n" +
               "A. Work Experience\n" +
               "Matched Skills: [List skills from work experience that match THIS JOB'S requirements]\n" +
               "Gaps: [List missing work experience requirements for THIS JOB]\n\n" +
               "B. Certificates\n" +
               "Matched Skills: [List relevant certificates that match THIS JOB'S requirements]\n" +
               "Gaps: [List missing certificate requirements for THIS JOB]\n\n" +
               "C. Projects\n" +
               "Matched Skills: [List relevant project skills that match THIS JOB'S requirements]\n" +
               "Gaps: [List missing project requirements for THIS JOB]\n\n" +
               "D. Technical Skills\n" +
               "Matched Skills: [List technical skills that match THIS JOB'S requirements]\n" +
               "Gaps: [List missing technical skills for THIS JOB]\n\n" +
               "IMPORTANT FORMATTING RULES:\n" +
               "1. Use square brackets [ ] around lists of items\n" +
               "2. Separate multiple items with commas within the brackets\n" +
               "3. If no skills match, write 'None' inside the brackets: [None]\n" +
               "4. If no gaps, write 'None' inside the brackets: [None]\n" +
               "5. Be specific and detailed in your analysis\n" +
               "6. Focus on skills and experience that directly relate to the job requirements\n" +
               "7. For Company and Role, extract the most relevant information from the job description\n" +
               "8. If company name is not clear, use 'Unknown Company'\n" +
               "9. If role is not clear, use 'Unknown Role'\n" +
               "10. IMPORTANT: Calculate Match Status based on the ATS score you provided above\n\n" +
               "EXAMPLE FORMAT:\n" +
               "2. ATS Score out of 10\n" +
               "Score: 8\n\n" +
               "3. Job Details\n" +
               "Company: [Google Inc]\n" +
               "Role: [Senior Software Engineer]\n" +
               "Match Status: [MATCHED]\n\n" +
               "A. Work Experience\n" +
               "Matched Skills: [Java development, Spring Framework, REST APIs, Database design]\n" +
               "Gaps: [No experience with microservices, No cloud platform experience]\n\n" +
               "Resume:\n" + resumeText + "\n\nJob Description:\n" + jdText;
    }
}
