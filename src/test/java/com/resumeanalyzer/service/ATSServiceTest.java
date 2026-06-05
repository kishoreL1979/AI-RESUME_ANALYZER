package com.resumeanalyzer.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.resumeanalyzer.dto.ATSResult;
import com.resumeanalyzer.dto.SkillMatchResult;

public class ATSServiceTest {

    private ATSService atsService;
    private SkillMatchingService skillMatchingService;

    @BeforeEach
    public void setUp() {
        atsService = new ATSService();
        skillMatchingService = new SkillMatchingService();
    }

    @Test
    public void testIsFresherDetection() {
        String fresherResume = "Computer Science and Business Systems undergraduate. Looking for entry level software developer role.";
        String experiencedResume = "Senior Software Architect with 8+ years of experience in enterprise systems.";

        Assertions.assertTrue(atsService.isFresher(fresherResume));
        Assertions.assertFalse(atsService.isFresher(experiencedResume));
    }

    @Test
    public void testEducationMajorMatchingAndSynonyms() {
        String jobDescription = "Looking for a candidate with Computer Science and Engineering degree";
        
        // Test various equivalent majors
        String resume1 = "Completed Computer Science and Business Systems degree in 2026";
        String resume2 = "B.Tech in Computer Science and Engineering from university";
        String resume3 = "Information Technology graduate";
        String resume4 = "Mechanical Engineering degree"; // should not match

        // We can check the ATS score or directly verify if isFresher is correct, and calculate scores
        SkillMatchResult skillMatch = new SkillMatchResult(List.of("Java"), List.of(), 100);

        ATSResult res1 = atsService.calculate(resume1, jobDescription, skillMatch);
        ATSResult res2 = atsService.calculate(resume2, jobDescription, skillMatch);
        ATSResult res3 = atsService.calculate(resume3, jobDescription, skillMatch);
        ATSResult res4 = atsService.calculate(resume4, jobDescription, skillMatch);

        // res1, res2, res3 should have high education score (part of breakdown)
        int ed1 = res1.getBreakdown().getOrDefault("education", 0);
        int ed2 = res2.getBreakdown().getOrDefault("education", 0);
        int ed3 = res3.getBreakdown().getOrDefault("education", 0);
        int ed4 = res4.getBreakdown().getOrDefault("education", 0);

        Assertions.assertTrue(ed1 >= ed4, "CSBS should match Computer Science requirement better than Mechanical");
        Assertions.assertTrue(ed2 >= ed4, "CSE should match Computer Science requirement better than Mechanical");
        Assertions.assertTrue(ed3 >= ed4, "IT should match Computer Science requirement better than Mechanical");
    }

    @Test
    public void testFresherFriendlyScoringModel() {
        // Resume has Java, Spring Boot, MySQL, React, JavaScript, Git, NPTEL Java Certification, CSBS Degree, Multiple Projects
        String resume = "Email: test@example.com Phone: 1234567890 \n" +
                        "Computer Science and Business Systems undergraduate. \n" +
                        "Skills: Java, Spring Boot, MySQL, React, JavaScript, Git. \n" +
                        "Projects: Developed AI Resume Analyzer. \n" +
                        "Certifications: NPTEL Programming in Java.";

        // Job description requires advanced tools as well
        String job = "Java Spring Boot MySQL React JavaScript Git AWS Docker Kubernetes Microservices";

        // Perform skill matching
        SkillMatchResult skillMatch = skillMatchingService.match(resume, job);

        // Expected matches: Java, Spring Boot, MySQL, React, JavaScript, Git
        // Expected missing: AWS, Docker, Kubernetes, Microservices
        Assertions.assertTrue(skillMatch.getMatchedSkills().contains("Java"));
        Assertions.assertTrue(skillMatch.getMatchedSkills().contains("Spring Boot"));
        Assertions.assertTrue(skillMatch.getMatchedSkills().contains("MySQL"));
        Assertions.assertTrue(skillMatch.getMatchedSkills().contains("React"));
        Assertions.assertTrue(skillMatch.getMatchedSkills().contains("JavaScript"));
        Assertions.assertTrue(skillMatch.getMatchedSkills().contains("Git"));
        Assertions.assertTrue(skillMatch.getMissingSkills().contains("AWS"));
        Assertions.assertTrue(skillMatch.getMissingSkills().contains("Docker"));
        Assertions.assertTrue(skillMatch.getMissingSkills().contains("Kubernetes"));
        Assertions.assertTrue(skillMatch.getMissingSkills().contains("Microservices"));

        // Calculate ATS Score
        ATSResult result = atsService.calculate(resume, job, skillMatch);
        int score = result.getScore();

        // Fresher model expected score should be 65-90
        System.out.println("Fresher ATS Score calculated: " + score);
        Assertions.assertTrue(score >= 65 && score <= 90, "Expected fresher ATS score between 65 and 90, but got: " + score);
    }

    @Test
    public void testSkillSynonymMatching() {
        String resume = "Experienced in GitHub, Spring MVC, Backend Services, AWS Cloud, Docker Containers";
        String job = "Git Spring Boot REST API AWS Docker";

        SkillMatchResult skillMatch = skillMatchingService.match(resume, job);

        // All skills should be matched due to synonyms:
        // GitHub -> Git
        // Spring MVC -> Spring Boot
        // Backend Services -> REST API
        // AWS Cloud -> AWS
        // Docker Containers -> Docker
        List<String> matched = skillMatch.getMatchedSkills();
        Assertions.assertTrue(matched.contains("Git"));
        Assertions.assertTrue(matched.contains("Spring Boot"));
        Assertions.assertTrue(matched.contains("REST API"));
        Assertions.assertTrue(matched.contains("AWS"));
        Assertions.assertTrue(matched.contains("Docker"));
        Assertions.assertTrue(skillMatch.getMissingSkills().isEmpty());
    }
}
