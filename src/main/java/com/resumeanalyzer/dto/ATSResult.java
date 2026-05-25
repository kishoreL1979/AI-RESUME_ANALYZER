package com.resumeanalyzer.dto;

import java.util.Map;

public class ATSResult {
    private int score;
    private Map<String, Integer> breakdown;

    public ATSResult() {}

    public ATSResult(int score, Map<String, Integer> breakdown) {
        this.score = score;
        this.breakdown = breakdown;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Map<String, Integer> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(Map<String, Integer> breakdown) {
        this.breakdown = breakdown;
    }
}
