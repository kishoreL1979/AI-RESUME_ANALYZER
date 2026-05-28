package com.resumeanalyzer.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.resumeanalyzer.dto.SkillMatchResult;

@Service
public class SkillMatchingService {

    private static final Logger log = LoggerFactory.getLogger(SkillMatchingService.class);

    public SkillMatchResult match(String resumeText, String jobDescription) {
        if (resumeText == null) resumeText = "";
        if (jobDescription == null) jobDescription = "";

        Set<String> resumeSkills = ResumeSkillExtractor.extractSkillsFromText(resumeText);
        Set<String> jobSkills = ResumeSkillExtractor.extractSkillsFromText(jobDescription);

        Set<String> normalizedResumeSkills = resumeSkills.stream()
            .map(ResumeSkillExtractor::normalizeSkill)
            .filter(Objects::nonNull)
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        Set<String> normalizedJobSkills = jobSkills.stream()
            .map(ResumeSkillExtractor::normalizeSkill)
            .filter(Objects::nonNull)
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        List<String> matched = normalizedResumeSkills.stream()
            .filter(normalizedJobSkills::contains)
            .sorted()
            .toList();

        List<String> missing = normalizedJobSkills.stream()
            .filter(s -> !normalizedResumeSkills.contains(s))
            .sorted()
            .toList();

        int matchPercentage = 0;
        if (!normalizedJobSkills.isEmpty()) {
            matchPercentage = (int) Math.round((matched.size() * 100.0) / normalizedJobSkills.size());
        }

        return new SkillMatchResult(matched, missing, matchPercentage);
    }
}
