package com.resumeanalyzer.dto;

import java.util.List;

public record ParsedProfile(
    PersonalInfo personalInfo,
    EducationDetail education,
    SkillsDetail skills,
    List<ProjectDetail> projects,
    List<CertificationDetail> certifications,
    List<ExperienceDetail> experience,
    List<String> languages
) {
    public static ParsedProfile createEmpty() {
        return new ParsedProfile(
            PersonalInfo.createEmpty(),
            EducationDetail.createEmpty(),
            SkillsDetail.createEmpty(),
            List.of(), List.of(), List.of(), List.of()
        );
    }
}
