package com.resumeanalyzer.dto;

import java.util.List;

public record ExperienceDetail(
    String role,
    String company,
    String duration,
    List<String> responsibilities
) {
    public static ExperienceDetail createEmpty() {
        return new ExperienceDetail("", "", "", List.of());
    }
}
