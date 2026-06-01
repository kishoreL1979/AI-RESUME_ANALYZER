package com.resumeanalyzer.dto;

public record EducationDetail(
    String degree,
    String department,
    String college,
    String university,
    String cgpa,
    String percentage,
    String graduationYear
) {
    public static EducationDetail createEmpty() {
        return new EducationDetail("", "", "", "", "", "", "");
    }
}
