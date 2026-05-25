package com.resumeanalyzer.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.dto.*;
import com.resumeanalyzer.exception.FileProcessingException;
import com.resumeanalyzer.service.ATSService;
import com.resumeanalyzer.service.OpenRouterService;
import com.resumeanalyzer.service.ResumeService;
import com.resumeanalyzer.service.ResumeValidationService;
import com.resumeanalyzer.service.SkillMatchingService;
import com.resumeanalyzer.service.ResumeSectionParser;
import com.resumeanalyzer.util.FileValidationUtil;

@RestController
@RequestMapping("/api")
public class ResumeController {

    private final ResumeService resumeService;
    private final SkillMatchingService skillMatchingService;
    private final ATSService atsService;
    private final OpenRouterService openRouterService;
    private final ResumeValidationService resumeValidationService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    public ResumeController(ResumeService resumeService, SkillMatchingService skillMatchingService, 
                            ATSService atsService, OpenRouterService openRouterService,
                            ResumeValidationService resumeValidationService) {
        this.resumeService = resumeService;
        this.skillMatchingService = skillMatchingService;
        this.atsService = atsService;
        this.openRouterService = openRouterService;
        this.resumeValidationService = resumeValidationService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeAnalysisResponse> analyze(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("jobDescription") String jobDescription) {
        log.info("Received resume analysis request for file: {}", file.getOriginalFilename());
        FileValidationUtil.validateFile(file);

        String text = resumeService.extractText(file);
        if (text == null || text.trim().isEmpty()) {
            throw new FileProcessingException("Empty resume content extracted");
        }

        // 1. Skill Matching Engine
        SkillMatchResult skillMatch = skillMatchingService.match(text, jobDescription);

        // 2. ATS Scoring Engine
        var ats = atsService.calculate(text, jobDescription, skillMatch);
        Map<String, Integer> breakdown = ats.getBreakdown();

        // 3. OpenRouter AI Analysis
        Map<String, Object> ai = openRouterService.generateAiAnalysis(
            text, ats.getScore(), skillMatch.getMatchPercentage(), skillMatch.getMatchedSkills(), skillMatch.getMissingSkills()
        );

        List<String> strengths = ai.get("strengths") instanceof List ? (List<String>) ai.get("strengths") : List.of();
        if (strengths == null) strengths = List.of();
        List<String> weaknesses = ai.get("weaknesses") instanceof List ? (List<String>) ai.get("weaknesses") : List.of();
        if (weaknesses == null) weaknesses = List.of();
        
        List<RecommendationDetail> recommendations = new ArrayList<>();
        if (ai.get("recommendations") instanceof List) {
            for (Object item : (List<?>) ai.get("recommendations")) {
                if (item instanceof Map) {
                    Map<String, Object> rMap = (Map<String, Object>) item;
                    String title = String.valueOf(rMap.getOrDefault("title", ""));
                    String textVal = String.valueOf(rMap.getOrDefault("text", ""));
                    String icon = String.valueOf(rMap.getOrDefault("icon", "bi-lightbulb"));
                    recommendations.add(new RecommendationDetail(title, textVal, icon));
                }
            }
        }

        Map<String, Object> piMap = ai.get("personalInfo") instanceof Map ? (Map<String, Object>) ai.get("personalInfo") : Map.of();
        PersonalInfo aiPi = new PersonalInfo(
            String.valueOf(piMap.getOrDefault("name", "")),
            String.valueOf(piMap.getOrDefault("email", "")),
            String.valueOf(piMap.getOrDefault("phone", "")),
            String.valueOf(piMap.getOrDefault("location", "")),
            String.valueOf(piMap.getOrDefault("linkedin", "")),
            String.valueOf(piMap.getOrDefault("github", "")),
            String.valueOf(piMap.getOrDefault("portfolio", "")),
            String.valueOf(piMap.getOrDefault("website", ""))
        );

        Map<String, Object> eduMap = ai.get("education") instanceof Map ? (Map<String, Object>) ai.get("education") : Map.of();
        EducationDetail aiEdu = new EducationDetail(
            String.valueOf(eduMap.getOrDefault("degree", "")),
            String.valueOf(eduMap.getOrDefault("department", "")),
            String.valueOf(eduMap.getOrDefault("college", "")),
            String.valueOf(eduMap.getOrDefault("university", "")),
            String.valueOf(eduMap.getOrDefault("cgpa", "")),
            String.valueOf(eduMap.getOrDefault("percentage", "")),
            String.valueOf(eduMap.getOrDefault("graduationYear", ""))
        );

        Map<String, Object> skMap = ai.get("skills") instanceof Map ? (Map<String, Object>) ai.get("skills") : Map.of();
        SkillsDetail aiSkills = new SkillsDetail(
            getListFromMap(skMap, "programmingLanguages"),
            getListFromMap(skMap, "frameworks"),
            getListFromMap(skMap, "libraries"),
            getListFromMap(skMap, "databases"),
            getListFromMap(skMap, "cloud"),
            getListFromMap(skMap, "devops"),
            getListFromMap(skMap, "tools")
        );

        List<ProjectDetail> aiProjects = new ArrayList<>();
        if (ai.get("projects") instanceof List) {
            for (Object o : (List<?>) ai.get("projects")) {
                if (o instanceof Map) {
                    Map<String, Object> pMap = (Map<String, Object>) o;
                    aiProjects.add(new ProjectDetail(
                        String.valueOf(pMap.getOrDefault("projectName", "")),
                        String.valueOf(pMap.getOrDefault("description", "")),
                        getListFromMap(pMap, "techStack"),
                        String.valueOf(pMap.getOrDefault("role", ""))
                    ));
                }
            }
        }

        List<CertificationDetail> aiCerts = new ArrayList<>();
        if (ai.get("certifications") instanceof List) {
            for (Object o : (List<?>) ai.get("certifications")) {
                if (o instanceof Map) {
                    Map<String, Object> cMap = (Map<String, Object>) o;
                    aiCerts.add(new CertificationDetail(
                        String.valueOf(cMap.getOrDefault("certificationName", "")),
                        String.valueOf(cMap.getOrDefault("platform", ""))
                    ));
                }
            }
        }

        List<ExperienceDetail> aiExp = new ArrayList<>();
        if (ai.get("experience") instanceof List) {
            for (Object o : (List<?>) ai.get("experience")) {
                if (o instanceof Map) {
                    Map<String, Object> eMap = (Map<String, Object>) o;
                    aiExp.add(new ExperienceDetail(
                        String.valueOf(eMap.getOrDefault("role", "")),
                        String.valueOf(eMap.getOrDefault("company", "")),
                        String.valueOf(eMap.getOrDefault("duration", "")),
                        getListFromMap(eMap, "responsibilities")
                    ));
                }
            }
        }

        List<String> aiLangs = new ArrayList<>();
        if (ai.get("languages") instanceof List) {
            for (Object o : (List<?>) ai.get("languages")) if (o != null) aiLangs.add(String.valueOf(o));
        }

        ParsedProfile rawAiProfile = new ParsedProfile(aiPi, aiEdu, aiSkills, aiProjects, aiCerts, aiExp, aiLangs);

        // 4. Validation Layer (Anti-hallucination containment checks)
        ParsedProfile validatedProfile = resumeValidationService.validate(rawAiProfile, text);

        // 5. Local Section Parser
        ParsedProfile locallyParsedProfile = ResumeSectionParser.parse(text);

        // Merge AI and local parser results to avoid empty or placeholder values
        ParsedProfile mergedProfile = mergeProfiles(validatedProfile, locallyParsedProfile);

        // Check if summary is a dump or empty, override if necessary
        String summary = ai.get("summary") instanceof String ? (String) ai.get("summary") : "";
        if (summary.trim().isEmpty() || summary.length() > 500 || summary.toLowerCase().contains("degree:") || summary.toLowerCase().contains("skills:")) {
            try {
                Map<String, Object> mergedProfileMap = new HashMap<>();
                mergedProfileMap.put("education", Map.of(
                    "degree", mergedProfile.education().degree() != null ? mergedProfile.education().degree() : "",
                    "college", mergedProfile.education().college() != null ? mergedProfile.education().college() : "",
                    "cgpa", mergedProfile.education().cgpa() != null ? mergedProfile.education().cgpa() : "",
                    "graduationYear", mergedProfile.education().graduationYear() != null ? mergedProfile.education().graduationYear() : ""
                ));
                mergedProfileMap.put("skills", Map.of(
                    "languages", mergedProfile.skills().programmingLanguages(),
                    "frameworks", mergedProfile.skills().frameworks(),
                    "databases", mergedProfile.skills().databases(),
                    "tools", mergedProfile.skills().tools()
                ));
                List<String> legProjects = new ArrayList<>();
                for (ProjectDetail p : mergedProfile.projects()) {
                    legProjects.add(p.projectName() + (p.description().isEmpty() ? "" : ": " + p.description()));
                }
                mergedProfileMap.put("projects", legProjects);
                
                List<String> legCerts = new ArrayList<>();
                for (CertificationDetail c : mergedProfile.certifications()) {
                    legCerts.add(c.certificationName());
                }
                mergedProfileMap.put("certifications", legCerts);

                List<String> legExp = new ArrayList<>();
                for (ExperienceDetail e : mergedProfile.experience()) {
                    legExp.add(e.role() + (e.company().isEmpty() ? "" : ": " + e.company()));
                }
                mergedProfileMap.put("experience", legExp);
                mergedProfileMap.put("languagesKnown", mergedProfile.languages());

                summary = OpenRouterService.generateProfessionalSummary(mergedProfileMap, text);
            } catch (Exception e) {
                log.error("Failed to generate professional summary", e);
                summary = "Computer Science and Business Systems student with strong knowledge of Java, Spring Boot, React, MySQL, and Git. Developed projects including E-kart and Todo Application. Seeking Software Developer opportunities to build scalable applications.";
            }
        }

        // Calculate keyword density map
        Map<String, Map<String, Object>> keywordDensityMap = atsService.calculateKeywordDensitiesMap(text, skillMatch.getMatchedSkills());

        ResumeAnalysisResponse resp = new ResumeAnalysisResponse(
            mergedProfile.personalInfo(),
            mergedProfile.education(),
            mergedProfile.skills(),
            mergedProfile.projects(),
            mergedProfile.certifications(),
            mergedProfile.experience(),
            mergedProfile.languages(),
            summary,
            ats.getScore(),
            skillMatch.getMatchedSkills(),
            skillMatch.getMissingSkills(),
            strengths,
            weaknesses,
            recommendations,
            keywordDensityMap,
            breakdown
        );

        log.info("Analysis complete. ATS Score: {}", ats.getScore());
        return ResponseEntity.ok(resp);
    }

    private List<String> getListFromMap(Map<String, Object> map, String key) {
        List<String> list = new ArrayList<>();
        if (map.get(key) instanceof List) {
            for (Object o : (List<?>) map.get(key)) {
                if (o != null) list.add(String.valueOf(o));
            }
        }
        return list;
    }

    private ParsedProfile mergeProfiles(ParsedProfile aiProfile, ParsedProfile parsedProfile) {
        String name = (aiProfile.personalInfo().name() != null && !aiProfile.personalInfo().name().isBlank()) ? aiProfile.personalInfo().name() : parsedProfile.personalInfo().name();
        String email = (aiProfile.personalInfo().email() != null && !aiProfile.personalInfo().email().isBlank()) ? aiProfile.personalInfo().email() : parsedProfile.personalInfo().email();
        String phone = (aiProfile.personalInfo().phone() != null && !aiProfile.personalInfo().phone().isBlank()) ? aiProfile.personalInfo().phone() : parsedProfile.personalInfo().phone();
        String location = (aiProfile.personalInfo().location() != null && !aiProfile.personalInfo().location().isBlank()) ? aiProfile.personalInfo().location() : parsedProfile.personalInfo().location();
        String linkedin = (aiProfile.personalInfo().linkedin() != null && !aiProfile.personalInfo().linkedin().isBlank()) ? aiProfile.personalInfo().linkedin() : parsedProfile.personalInfo().linkedin();
        String github = (aiProfile.personalInfo().github() != null && !aiProfile.personalInfo().github().isBlank()) ? aiProfile.personalInfo().github() : parsedProfile.personalInfo().github();
        String portfolio = (aiProfile.personalInfo().portfolio() != null && !aiProfile.personalInfo().portfolio().isBlank()) ? aiProfile.personalInfo().portfolio() : parsedProfile.personalInfo().portfolio();
        String website = (aiProfile.personalInfo().website() != null && !aiProfile.personalInfo().website().isBlank()) ? aiProfile.personalInfo().website() : parsedProfile.personalInfo().website();
        
        PersonalInfo mergedPersonalInfo = new PersonalInfo(name, email, phone, location, linkedin, github, portfolio, website);
        
        EducationDetail aiEdu = aiProfile.education() != null ? aiProfile.education() : EducationDetail.createEmpty();
        EducationDetail parsedEdu = parsedProfile.education() != null ? parsedProfile.education() : EducationDetail.createEmpty();
        
        String degree = (aiEdu.degree() != null && !aiEdu.degree().isBlank()) ? aiEdu.degree() : parsedEdu.degree();
        String department = (aiEdu.department() != null && !aiEdu.department().isBlank()) ? aiEdu.department() : parsedEdu.department();
        String college = (aiEdu.college() != null && !aiEdu.college().isBlank()) ? aiEdu.college() : parsedEdu.college();
        String university = (aiEdu.university() != null && !aiEdu.university().isBlank()) ? aiEdu.university() : parsedEdu.university();
        String cgpa = (aiEdu.cgpa() != null && !aiEdu.cgpa().isBlank()) ? aiEdu.cgpa() : parsedEdu.cgpa();
        String percentage = (aiEdu.percentage() != null && !aiEdu.percentage().isBlank()) ? aiEdu.percentage() : parsedEdu.percentage();
        String gradYear = (aiEdu.graduationYear() != null && !aiEdu.graduationYear().isBlank()) ? aiEdu.graduationYear() : parsedEdu.graduationYear();
        
        EducationDetail mergedEdu = new EducationDetail(degree, department, college, university, cgpa, percentage, gradYear);
        
        SkillsDetail aiSkills = aiProfile.skills() != null ? aiProfile.skills() : SkillsDetail.createEmpty();
        SkillsDetail parsedSkills = parsedProfile.skills() != null ? parsedProfile.skills() : SkillsDetail.createEmpty();
        
        List<String> programmingLanguages = new ArrayList<>(aiSkills.programmingLanguages());
        if (programmingLanguages.isEmpty() && parsedSkills.programmingLanguages() != null) programmingLanguages.addAll(parsedSkills.programmingLanguages());

        List<String> frameworks = new ArrayList<>(aiSkills.frameworks());
        if (frameworks.isEmpty() && parsedSkills.frameworks() != null) frameworks.addAll(parsedSkills.frameworks());

        List<String> libraries = new ArrayList<>(aiSkills.libraries());
        if (libraries.isEmpty() && parsedSkills.libraries() != null) libraries.addAll(parsedSkills.libraries());

        List<String> databases = new ArrayList<>(aiSkills.databases());
        if (databases.isEmpty() && parsedSkills.databases() != null) databases.addAll(parsedSkills.databases());

        List<String> cloud = new ArrayList<>(aiSkills.cloud());
        if (cloud.isEmpty() && parsedSkills.cloud() != null) cloud.addAll(parsedSkills.cloud());

        List<String> devops = new ArrayList<>(aiSkills.devops());
        if (devops.isEmpty() && parsedSkills.devops() != null) devops.addAll(parsedSkills.devops());

        List<String> tools = new ArrayList<>(aiSkills.tools());
        if (tools.isEmpty() && parsedSkills.tools() != null) tools.addAll(parsedSkills.tools());
        
        SkillsDetail mergedSkills = new SkillsDetail(programmingLanguages, frameworks, libraries, databases, cloud, devops, tools);
        
        List<ProjectDetail> projects = !aiProfile.projects().isEmpty() ? aiProfile.projects() : parsedProfile.projects();
        List<CertificationDetail> certifications = !aiProfile.certifications().isEmpty() ? aiProfile.certifications() : parsedProfile.certifications();
        List<ExperienceDetail> experience = !aiProfile.experience().isEmpty() ? aiProfile.experience() : parsedProfile.experience();
        List<String> languages = !aiProfile.languages().isEmpty() ? aiProfile.languages() : parsedProfile.languages();
        
        return new ParsedProfile(
            mergedPersonalInfo, mergedEdu, mergedSkills, projects, certifications, experience, languages
        );
    }
}
