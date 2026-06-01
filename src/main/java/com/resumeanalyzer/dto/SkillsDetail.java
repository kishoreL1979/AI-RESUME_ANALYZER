package com.resumeanalyzer.dto;

import java.util.List;

public record SkillsDetail(
    List<String> programmingLanguages,
    List<String> frameworks,
    List<String> libraries,
    List<String> databases,
    List<String> cloud,
    List<String> devops,
    List<String> tools
) {
    public static SkillsDetail createEmpty() {
        return new SkillsDetail(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
