package com.resumeanalyzer.constants;

import java.util.Arrays;
import java.util.List;

public class AppConstants {
    public static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
}
