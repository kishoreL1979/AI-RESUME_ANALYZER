package com.resumeanalyzer.dto;

import java.util.List;

public record ProjectDetail(
    String projectName,
    String description,
    List<String> techStack,
    String role
) {
    public static ProjectDetail createEmpty() {
        return new ProjectDetail("", "", List.of(), "");
    }
}
