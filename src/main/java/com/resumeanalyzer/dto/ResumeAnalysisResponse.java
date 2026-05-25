package com.resumeanalyzer.dto;

import java.util.List;
import java.util.Map;

public record ResumeAnalysisResponse(
    PersonalInfo personalInfo,
    EducationDetail education,
    SkillsDetail skills,
    List<ProjectDetail> projects,
    List<CertificationDetail> certifications,
    List<ExperienceDetail> experience,
    List<String> languages,
    String summary,
    int atsScore,
    List<String> matchedSkills,
    List<String> missingSkills,
    List<String> strengths,
    List<String> weaknesses,
    List<RecommendationDetail> recommendations,
    Map<String, Map<String, Object>> keywordDensity,
    Map<String, Integer> atsBreakdown
) {}
