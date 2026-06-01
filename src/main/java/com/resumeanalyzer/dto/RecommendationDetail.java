package com.resumeanalyzer.dto;

public record RecommendationDetail(
    String title,
    String text,
    String icon
) {
    public static RecommendationDetail createEmpty() {
        return new RecommendationDetail("", "", "bi-lightbulb");
    }
}
