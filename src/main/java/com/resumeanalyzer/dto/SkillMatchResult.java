package com.resumeanalyzer.dto;

import java.util.List;

public class SkillMatchResult {
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private int matchPercentage;

    public SkillMatchResult() {}

    public SkillMatchResult(List<String> matchedSkills, List<String> missingSkills, int matchPercentage) {
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.matchPercentage = matchPercentage;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public int getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(int matchPercentage) {
        this.matchPercentage = matchPercentage;
    }
}
