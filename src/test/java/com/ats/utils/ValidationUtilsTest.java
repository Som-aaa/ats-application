package com.ats.utils;

import com.ats.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void testValidateFile_ValidPdf() {
        MockMultipartFile validFile = new MockMultipartFile(
            "resume",
            "test.pdf",
            "application/pdf",
            "Test content".getBytes()
        );

        assertDoesNotThrow(() -> ValidationUtils.validateFile(validFile, "resume"));
    }

    @Test
    void testValidateFile_ValidDocx() {
        MockMultipartFile validFile = new MockMultipartFile(
            "resume",
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "Test content".getBytes()
        );

        assertDoesNotThrow(() -> ValidationUtils.validateFile(validFile, "resume"));
    }

    @Test
    void testValidateFile_NullFile() {
        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateFile(null, "resume");
        });
    }

    @Test
    void testValidateFile_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "resume",
            "test.pdf",
            "application/pdf",
            new byte[0]
        );

        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateFile(emptyFile, "resume");
        });
    }

    @Test
    void testValidateFile_InvalidFileType() {
        MockMultipartFile invalidFile = new MockMultipartFile(
            "resume",
            "test.txt",
            "text/plain",
            "Test content".getBytes()
        );

        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateFile(invalidFile, "resume");
        });
    }

    @Test
    void testValidateFile_FileTooLarge() {
        // Create a file larger than 10MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
            "resume",
            "test.pdf",
            "application/pdf",
            largeContent
        );

        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateFile(largeFile, "resume");
        });
    }

    @Test
    void testValidateFiles_ValidFiles() {
        MockMultipartFile[] validFiles = {
            new MockMultipartFile("resume1", "test1.pdf", "application/pdf", "Content1".getBytes()),
            new MockMultipartFile("resume2", "test2.pdf", "application/pdf", "Content2".getBytes())
        };

        assertDoesNotThrow(() -> ValidationUtils.validateFiles(validFiles, "resumes"));
    }

    @Test
    void testValidateFiles_TooManyFiles() {
        MockMultipartFile[] tooManyFiles = new MockMultipartFile[21]; // More than 20
        for (int i = 0; i < 21; i++) {
            tooManyFiles[i] = new MockMultipartFile(
                "resume" + i,
                "test" + i + ".pdf",
                "application/pdf",
                "Content".getBytes()
            );
        }

        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateFiles(tooManyFiles, "resumes");
        });
    }

    @Test
    void testValidateText_ValidText() {
        assertDoesNotThrow(() -> {
            ValidationUtils.validateText("Valid text content", "field", true);
        });
    }

    @Test
    void testValidateText_EmptyRequiredText() {
        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateText("", "field", true);
        });
    }

    @Test
    void testValidateText_TextTooLong() {
        String longText = "a".repeat(100001); // More than 100k characters

        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateText(longText, "field", true);
        });
    }

    @Test
    void testValidateText_XSSContent() {
        String xssContent = "<script>alert('xss')</script>";

        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateText(xssContent, "field", true);
        });
    }

    @Test
    void testValidateText_JavaScriptContent() {
        String jsContent = "javascript:alert('xss')";

        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateText(jsContent, "field", true);
        });
    }

    @Test
    void testSanitizeText_ValidText() {
        String input = "Valid text content";
        String result = ValidationUtils.sanitizeText(input);
        assertEquals(input, result);
    }

    @Test
    void testSanitizeText_XSSContent() {
        String input = "<script>alert('xss')</script>Valid content";
        String result = ValidationUtils.sanitizeText(input);
        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("Valid content"));
    }

    @Test
    void testSanitizeText_JavaScriptContent() {
        String input = "javascript:alert('xss')Valid content";
        String result = ValidationUtils.sanitizeText(input);
        assertFalse(result.contains("javascript:"));
        assertTrue(result.contains("Valid content"));
    }

    @Test
    void testSanitizeText_NormalizeWhitespace() {
        String input = "Text   with    multiple    spaces";
        String result = ValidationUtils.sanitizeText(input);
        assertEquals("Text with multiple spaces", result);
    }

    @Test
    void testValidateClientId_ValidClientId() {
        assertDoesNotThrow(() -> {
            ValidationUtils.validateClientId("client123");
        });
    }

    @Test
    void testValidateClientId_ValidClientIdWithUnderscore() {
        assertDoesNotThrow(() -> {
            ValidationUtils.validateClientId("client_123");
        });
    }

    @Test
    void testValidateClientId_ValidClientIdWithHyphen() {
        assertDoesNotThrow(() -> {
            ValidationUtils.validateClientId("client-123");
        });
    }

    @Test
    void testValidateClientId_InvalidClientIdWithSpecialChars() {
        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateClientId("client@123");
        });
    }

    @Test
    void testValidateClientId_ClientIdTooLong() {
        String longClientId = "a".repeat(101); // More than 100 characters

        assertThrows(ValidationException.class, () -> {
            ValidationUtils.validateClientId(longClientId);
        });
    }

    @Test
    void testValidateClientId_NullClientId() {
        assertDoesNotThrow(() -> {
            ValidationUtils.validateClientId(null);
        });
    }

    @Test
    void testValidateClientId_EmptyClientId() {
        assertDoesNotThrow(() -> {
            ValidationUtils.validateClientId("");
        });
    }
}
