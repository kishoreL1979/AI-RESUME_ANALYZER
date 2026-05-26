package com.resumeanalyzer.util;

import com.resumeanalyzer.constants.AppConstants;
import org.springframework.web.multipart.MultipartFile;

public class FileValidationUtil {

    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > AppConstants.MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !AppConstants.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only PDF and DOCX are allowed");
        }
    }
}
