package com.resumeanalyzer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.resumeanalyzer.config.OpenRouterConfig;

@Service
public class OpenRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);

    private final RestTemplate restTemplate;
    private final OpenRouterConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenRouterService(RestTemplate restTemplate, OpenRouterConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public Map<String, Object> generateAiAnalysis(String resumeText, int atsScore, int matchPercentage,
                                                  Object matchedSkills, Object missingSkills) {
        try {
            if (!config.isEnabled() || config.getApiKey() == null || config.getApiKey().isBlank() || "YOUR_KEY".equals(config.getApiKey())) {
                log.info("OpenRouter disabled or API key is default; using fallback AI response");
                return fallback(resumeText, matchedSkills, missingSkills);
            }
            String prompt = buildPrompt(resumeText, atsScore, matchPercentage, matchedSkills, missingSkills);

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModel());
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            body.put("messages", List.of(message));
            body.put("temperature", 0.1); 

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("HTTP-Referer", "http://localhost:8086");
            headers.set("X-Title", "AI Resume Analyzer");

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            log.info("Sending request to OpenRouter API using model: {}", config.getModel());
            String resp = restTemplate.postForObject(config.getBaseUrl(), req, String.class);
            JsonNode root = mapper.readTree(resp);

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                log.warn("OpenRouter returned no choices: {}", resp);
                return fallback(resumeText, matchedSkills, missingSkills);
            }

            JsonNode first = choices.get(0);
            JsonNode messageNode = first.path("message");
            String content = null;
            if (messageNode.isObject()) content = messageNode.path("content").asText(null);
            if (content == null || content.isEmpty()) content = first.path("text").asText(null);
            if (content == null) {
                log.warn("OpenRouter returned empty content: {}", resp);
                return fallback(resumeText, matchedSkills, missingSkills);
            }

            String jsonText = extractJson(content);
            if (jsonText == null) {
                log.warn("Could not extract JSON from OpenRouter content. Falling back. Content: {}", content);
                return fallback(resumeText, matchedSkills, missingSkills);
            }

            JsonNode data = mapper.readTree(jsonText);
            if (!validateSchema(data)) {
                log.warn("OpenRouter returned JSON but schema invalid. Falling back. JSON: {}", data.toString());
                return fallback(resumeText, matchedSkills, missingSkills);
            }

            return parseAndNormalizeResponse(data);
        } catch (Exception e) {
            log.error("OpenRouter call failed, returning fallback structure", e);
            return fallback(resumeText, matchedSkills, missingSkills);
        }
    }

    private String buildPrompt(String resumeText, int atsScore, int matchPercentage, Object matchedSkills, Object missingSkills) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert ATS Resume Analyzer, Parser, and Recruiter. Analyze the provided resume text and extract candidate's profile details.\n");
        sb.append("You must return a single, valid JSON object with the following flat schema and absolutely nothing else. Do not include markdown code wrappers (like ```json), notes, explanations, or text outside the JSON block.\n");
        sb.append("CRITICAL: Do NOT invent or hallucinate any profile data. If a field is not found in the resume, you must set its value to empty string \"\" or empty list []. No fields should be null.\n\n");
        sb.append("Required JSON Schema:\n");
        sb.append("{\n");
        sb.append("  \"personalInfo\": {\n");
        sb.append("    \"name\": \"Full Name (string)\",\n");
        sb.append("    \"email\": \"Email Address (string)\",\n");
        sb.append("    \"phone\": \"Phone Number (string)\",\n");
        sb.append("    \"location\": \"City, State (string)\",\n");
        sb.append("    \"linkedin\": \"LinkedIn Profile URL (string)\",\n");
        sb.append("    \"github\": \"GitHub Profile URL (string)\",\n");
        sb.append("    \"portfolio\": \"Portfolio URL (string)\",\n");
        sb.append("    \"website\": \"Personal Website URL (string)\"\n");
        sb.append("  },\n");
        sb.append("  \"education\": {\n");
        sb.append("    \"degree\": \"Degree title, e.g. B.Tech Computer Science and Business Systems (string)\",\n");
        sb.append("    \"department\": \"Department, e.g. Computer Science (string)\",\n");
        sb.append("    \"college\": \"College name (string)\",\n");
        sb.append("    \"university\": \"University name (string)\",\n");
        sb.append("    \"cgpa\": \"CGPA, e.g. 8.09 (string)\",\n");
        sb.append("    \"percentage\": \"Percentage, e.g. 85% (string)\",\n");
        sb.append("    \"graduationYear\": \"Graduation Year (string)\"\n");
        sb.append("  },\n");
        sb.append("  \"skills\": {\n");
        sb.append("    \"programmingLanguages\": [\"Java\", \"JavaScript\", ...],\n");
        sb.append("    \"frameworks\": [\"Spring Boot\", \"React\", ...],\n");
        sb.append("    \"libraries\": [\"React Router\", ...],\n");
        sb.append("    \"databases\": [\"MySQL\", ...],\n");
        sb.append("    \"cloud\": [\"AWS\", ...],\n");
        sb.append("    \"devops\": [\"Docker\", ...],\n");
        sb.append("    \"tools\": [\"Git\", \"Maven\", ...]\n");
        sb.append("  },\n");
        sb.append("  \"projects\": [\n");
        sb.append("    {\n");
        sb.append("      \"projectName\": \"Project Name (string)\",\n");
        sb.append("      \"description\": \"Project Description (string)\",\n");
        sb.append("      \"techStack\": [\"Java\", \"Spring Boot\", ...],\n");
        sb.append("      \"role\": \"Role (string)\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"certifications\": [\n");
        sb.append("    {\n");
        sb.append("      \"certificationName\": \"Certification Name (string)\",\n");
        sb.append("      \"platform\": \"Platform, e.g. Coursera (string)\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"experience\": [\n");
        sb.append("    {\n");
        sb.append("      \"role\": \"Role, e.g. Software Engineer Intern (string)\",\n");
        sb.append("      \"company\": \"Company Name (string)\",\n");
        sb.append("      \"duration\": \"Duration, e.g. June 2025 - August 2025 (string)\",\n");
        sb.append("      \"responsibilities\": [\"Responsibility 1\", \"Responsibility 2\", ...]\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"languages\": [\"Language 1\", \"Language 2\", ...],\n");
        sb.append("  \"summary\": \"A professional, recruiter-friendly summary of exactly 3 to 5 lines. It must cover the candidate's education, experience level, core skills, key projects, certifications, and career focus. Do not dump raw text. (string)\",\n");
        sb.append("  \"strengths\": [\"Strength 1\", \"Strength 2\", ...],\n");
        sb.append("  \"weaknesses\": [\"Weakness 1\", \"Weakness 2\", ...],\n");
        sb.append("  \"recommendations\": [\n");
        sb.append("    { \"title\": \"Suggestion Title\", \"text\": \"Detailed actionable recommendation text\", \"icon\": \"bi-lightbulb\" }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("Suggestions' icon property must be one of: bi-key, bi-lightbulb, bi-layout-text-window, bi-star.\n\n");
        sb.append("Resume Text:\n");
        sb.append(resumeText.substring(0, Math.min(25000, resumeText.length()))).append("\n\n");
        sb.append("Context Data:\n");
        sb.append("ATS Score: ").append(atsScore).append("\n");
        sb.append("Match Percentage: ").append(matchPercentage).append("\n");
        sb.append("Matched Skills: ").append(matchedSkills).append("\n");
        sb.append("Missing Skills: ").append(missingSkills).append("\n");
        return sb.toString();
    }

    private String extractJson(String content) {
        content = content.trim();
        if (content.startsWith("{") && content.endsWith("}")) return content;

        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return null;
    }

    private boolean validateSchema(JsonNode data) {
        if (!data.isObject()) return false;
        ObjectNode obj = (ObjectNode) data;
        
        JsonNode summary = obj.get("summary");
        JsonNode strengths = obj.get("strengths");
        JsonNode weaknesses = obj.get("weaknesses");
        JsonNode recommendations = obj.get("recommendations");

        if (summary == null || !summary.isTextual()) return false;
        if (strengths == null || !strengths.isArray()) return false;
        if (weaknesses == null || !weaknesses.isArray()) return false;
        if (recommendations == null || !recommendations.isArray()) return false;

        boolean hasProfile = obj.has("profile") && obj.get("profile").isObject();
        if (hasProfile) {
            ObjectNode pObj = (ObjectNode) obj.get("profile");
            if (!pObj.has("name") || !pObj.has("email") || !pObj.has("phone")) return false;
            return true;
        } else {
            if (!obj.has("personalInfo") || !obj.has("education") || !obj.has("skills")) return false;
            return true;
        }
    }

    private Map<String, Object> parseAndNormalizeResponse(JsonNode data) {
        Map<String, Object> out = new HashMap<>();
        
        out.put("summary", data.path("summary").asText(""));
        
        List<String> strengths = new ArrayList<>();
        if (data.has("strengths") && data.get("strengths").isArray()) {
            for (JsonNode n : data.get("strengths")) strengths.add(n.asText());
        }
        out.put("strengths", strengths);
        
        List<String> weaknesses = new ArrayList<>();
        if (data.has("weaknesses") && data.get("weaknesses").isArray()) {
            for (JsonNode n : data.get("weaknesses")) weaknesses.add(n.asText());
        }
        out.put("weaknesses", weaknesses);
        
        List<Map<String, String>> recommendations = new ArrayList<>();
        if (data.has("recommendations") && data.get("recommendations").isArray()) {
            for (JsonNode n : data.get("recommendations")) {
                if (n.isObject()) {
                    Map<String, String> rec = new HashMap<>();
                    rec.put("title", n.path("title").asText(""));
                    rec.put("text", n.path("text").asText(""));
                    rec.put("icon", n.path("icon").asText("bi-lightbulb"));
                    recommendations.add(rec);
                }
            }
        }
        out.put("recommendations", recommendations);

        boolean hasProfile = data.has("profile") && data.get("profile").isObject();
        JsonNode profNode = hasProfile ? data.get("profile") : data;

        Map<String, String> pi = new HashMap<>();
        if (hasProfile) {
            pi.put("name", profNode.path("name").isTextual() ? profNode.path("name").asText() : null);
            pi.put("email", profNode.path("email").isTextual() ? profNode.path("email").asText() : null);
            pi.put("phone", profNode.path("phone").isTextual() ? profNode.path("phone").asText() : null);
            pi.put("location", profNode.path("location").isTextual() ? profNode.path("location").asText() : null);
            pi.put("linkedin", profNode.path("linkedin").isTextual() ? profNode.path("linkedin").asText() : null);
            pi.put("github", profNode.path("github").isTextual() ? profNode.path("github").asText() : null);
            pi.put("portfolio", null);
            pi.put("website", null);
        } else {
            JsonNode piNode = data.path("personalInfo");
            pi.put("name", piNode.path("name").isTextual() ? piNode.path("name").asText() : null);
            pi.put("email", piNode.path("email").isTextual() ? piNode.path("email").asText() : null);
            pi.put("phone", piNode.path("phone").isTextual() ? piNode.path("phone").asText() : null);
            pi.put("location", piNode.path("location").isTextual() ? piNode.path("location").asText() : null);
            pi.put("linkedin", piNode.path("linkedin").isTextual() ? piNode.path("linkedin").asText() : null);
            pi.put("github", piNode.path("github").isTextual() ? piNode.path("github").asText() : null);
            pi.put("portfolio", piNode.path("portfolio").isTextual() ? piNode.path("portfolio").asText() : null);
            pi.put("website", piNode.path("website").isTextual() ? piNode.path("website").asText() : null);
        }
        out.put("personalInfo", pi);

        Map<String, String> edu = new HashMap<>();
        if (hasProfile) {
            JsonNode eduNode = profNode.path("education");
            if (eduNode.isObject()) {
                edu.put("degree", eduNode.path("degree").isTextual() ? eduNode.path("degree").asText() : null);
                edu.put("college", eduNode.path("college").isTextual() ? eduNode.path("college").asText() : null);
                edu.put("cgpa", eduNode.path("cgpa").isTextual() ? eduNode.path("cgpa").asText() : null);
                edu.put("graduationYear", eduNode.path("graduationYear").isTextual() ? eduNode.path("graduationYear").asText() : null);
            } else if (eduNode.isTextual()) {
                edu.put("degree", eduNode.asText());
                edu.put("college", null);
                edu.put("cgpa", null);
                edu.put("graduationYear", null);
            }
            edu.put("department", null);
            edu.put("university", null);
            edu.put("percentage", null);
        } else {
            JsonNode eduNode = data.path("education");
            edu.put("degree", eduNode.path("degree").isTextual() ? eduNode.path("degree").asText() : null);
            edu.put("department", eduNode.path("department").isTextual() ? eduNode.path("department").asText() : null);
            edu.put("college", eduNode.path("college").isTextual() ? eduNode.path("college").asText() : null);
            edu.put("university", eduNode.path("university").isTextual() ? eduNode.path("university").asText() : null);
            edu.put("cgpa", eduNode.path("cgpa").isTextual() ? eduNode.path("cgpa").asText() : null);
            edu.put("percentage", eduNode.path("percentage").isTextual() ? eduNode.path("percentage").asText() : null);
            edu.put("graduationYear", eduNode.path("graduationYear").isTextual() ? eduNode.path("graduationYear").asText() : null);
        }
        out.put("education", edu);

        Map<String, List<String>> skills = new HashMap<>();
        if (hasProfile) {
            JsonNode skNode = profNode.path("skills");
            List<String> list = new ArrayList<>();
            if (skNode.isArray()) {
                for (JsonNode n : skNode) list.add(n.asText());
            } else if (skNode.isObject()) {
                if (skNode.path("languages").isArray()) {
                    for (JsonNode n : skNode.path("languages")) list.add(n.asText());
                }
                if (skNode.path("frameworks").isArray()) {
                    for (JsonNode n : skNode.path("frameworks")) list.add(n.asText());
                }
                if (skNode.path("databases").isArray()) {
                    for (JsonNode n : skNode.path("databases")) list.add(n.asText());
                }
                if (skNode.path("tools").isArray()) {
                    for (JsonNode n : skNode.path("tools")) list.add(n.asText());
                }
            }
            skills.put("programmingLanguages", list);
            skills.put("frameworks", new ArrayList<>());
            skills.put("libraries", new ArrayList<>());
            skills.put("databases", new ArrayList<>());
            skills.put("cloud", new ArrayList<>());
            skills.put("devops", new ArrayList<>());
            skills.put("tools", new ArrayList<>());
        } else {
            JsonNode skNode = data.path("skills");
            skills.put("programmingLanguages", getList(skNode.path("programmingLanguages")));
            skills.put("frameworks", getList(skNode.path("frameworks")));
            skills.put("libraries", getList(skNode.path("libraries")));
            skills.put("databases", getList(skNode.path("databases")));
            skills.put("cloud", getList(skNode.path("cloud")));
            skills.put("devops", getList(skNode.path("devops")));
            skills.put("tools", getList(skNode.path("tools")));
        }
        out.put("skills", skills);

        List<Map<String, Object>> projects = new ArrayList<>();
        JsonNode projNode = profNode.path("projects");
        if (projNode.isArray()) {
            for (JsonNode n : projNode) {
                if (n.isObject()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("projectName", n.path("projectName").asText(""));
                    p.put("description", n.path("description").asText(""));
                    p.put("techStack", getList(n.path("techStack")));
                    p.put("role", n.path("role").asText(""));
                    projects.add(p);
                } else if (n.isTextual()) {
                    String str = n.asText();
                    Map<String, Object> p = new HashMap<>();
                    String name = str.contains(":") ? str.split(":")[0].trim() : str;
                    String desc = str.contains(":") ? str.substring(str.indexOf(":") + 1).trim() : "";
                    p.put("projectName", name);
                    p.put("description", desc);
                    p.put("techStack", new ArrayList<String>());
                    p.put("role", "");
                    projects.add(p);
                }
            }
        }
        out.put("projects", projects);

        List<Map<String, String>> certs = new ArrayList<>();
        JsonNode certsNode = profNode.path("certifications");
        if (certsNode.isArray()) {
            for (JsonNode n : certsNode) {
                if (n.isObject()) {
                    Map<String, String> c = new HashMap<>();
                    c.put("certificationName", n.path("certificationName").asText(""));
                    c.put("platform", n.path("platform").asText(""));
                    certs.add(c);
                } else if (n.isTextual()) {
                    String str = n.asText();
                    Map<String, String> c = new HashMap<>();
                    c.put("certificationName", str);
                    c.put("platform", "");
                    certs.add(c);
                }
            }
        }
        out.put("certifications", certs);

        List<Map<String, Object>> exp = new ArrayList<>();
        JsonNode expNode = profNode.path("experience");
        if (expNode.isArray()) {
            for (JsonNode n : expNode) {
                if (n.isObject()) {
                    Map<String, Object> e = new HashMap<>();
                    e.put("role", n.path("role").asText(""));
                    e.put("company", n.path("company").asText(""));
                    e.put("duration", n.path("duration").asText(""));
                    e.put("responsibilities", getList(n.path("responsibilities")));
                    exp.add(e);
                } else if (n.isTextual()) {
                    String str = n.asText();
                    Map<String, Object> e = new HashMap<>();
                    String role = str.contains(":") ? str.split(":")[0].trim() : str;
                    String desc = str.contains(":") ? str.substring(str.indexOf(":") + 1).trim() : "";
                    e.put("role", role);
                    e.put("company", desc);
                    e.put("duration", "");
                    e.put("responsibilities", new ArrayList<String>());
                    exp.add(e);
                }
            }
        }
        out.put("experience", exp);

        List<String> langs = getList(profNode.has("languages") ? profNode.path("languages") : profNode.path("languagesKnown"));
        out.put("languages", langs);

        Map<String, Object> legacyProfile = new HashMap<>();
        legacyProfile.put("name", pi.get("name"));
        legacyProfile.put("email", pi.get("email"));
        legacyProfile.put("phone", pi.get("phone"));
        legacyProfile.put("location", pi.get("location"));
        legacyProfile.put("linkedin", pi.get("linkedin"));
        legacyProfile.put("github", pi.get("github"));
        legacyProfile.put("education", edu.get("degree"));
        
        List<String> legacySkills = new ArrayList<>();
        legacySkills.addAll(skills.get("programmingLanguages"));
        legacySkills.addAll(skills.get("frameworks"));
        legacySkills.addAll(skills.get("libraries"));
        legacySkills.addAll(skills.get("databases"));
        legacySkills.addAll(skills.get("cloud"));
        legacySkills.addAll(skills.get("devops"));
        legacySkills.addAll(skills.get("tools"));
        legacyProfile.put("skills", legacySkills);
        
        List<String> legProj = new ArrayList<>();
        for (Map<String, Object> p : projects) {
            String name = (String) p.get("projectName");
            String desc = (String) p.get("description");
            legProj.add(name + (desc.isEmpty() ? "" : ": " + desc));
        }
        legacyProfile.put("projects", legProj);

        List<String> legCerts = new ArrayList<>();
        for (Map<String, String> c : certs) {
            legCerts.add(c.get("certificationName"));
        }
        legacyProfile.put("certifications", legCerts);

        List<String> legExp = new ArrayList<>();
        for (Map<String, Object> e : exp) {
            String role = (String) e.get("role");
            String comp = (String) e.get("company");
            legExp.add(role + (comp.isEmpty() ? "" : ": " + comp));
        }
        legacyProfile.put("experience", legExp);
        legacyProfile.put("languagesKnown", langs);
        
        out.put("profile", legacyProfile);
        
        return out;
    }

    private List<String> getList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode n : node) list.add(n.asText());
        }
        return list;
    }

    public Map<String, Object> fallback(String resumeText, Object matchedSkills, Object missingSkills) {
        Map<String, Object> out = new HashMap<>();

        List<String> matched = new ArrayList<>();
        if (matchedSkills instanceof List) {
            for (Object o : (List<?>) matchedSkills) {
                matched.add(String.valueOf(o));
            }
        }
        List<String> missing = new ArrayList<>();
        if (missingSkills instanceof List) {
            for (Object o : (List<?>) missingSkills) {
                missing.add(String.valueOf(o));
            }
        }

        List<String> strengths = new ArrayList<>();
        if (!matched.isEmpty()) {
            strengths.add("Strong technical skill alignment with job requirements: " + String.join(", ", matched.subList(0, Math.min(matched.size(), 3))));
        } else {
            strengths.add("Professional resume formatting and clear structure.");
        }
        strengths.add("Active learning indicated by project records.");
        out.put("strengths", strengths);

        List<String> weaknesses = new ArrayList<>();
        if (!missing.isEmpty()) {
            weaknesses.add("Missing primary tools: " + String.join(", ", missing.subList(0, Math.min(missing.size(), 3))));
        }
        weaknesses.add("Quantifiable impact descriptions can be added to achievements.");
        out.put("weaknesses", weaknesses);

        List<Map<String, String>> recommendations = new ArrayList<>();
        Map<String, String> r1 = new HashMap<>();
        r1.put("title", "Optimize Keywords");
        r1.put("text", "Naturally include missing skills such as " + (missing.isEmpty() ? "Docker" : missing.get(0)) + " in your work or projects description.");
        r1.put("icon", "bi-key");
        recommendations.add(r1);

        Map<String, String> r2 = new HashMap<>();
        r2.put("title", "Quantify Impact");
        r2.put("text", "Mention metrics like 'speed increase of 30%' or 'reduction in query execution time' to highlight project impact.");
        r2.put("icon", "bi-lightbulb");
        recommendations.add(r2);
        out.put("recommendations", recommendations);

        Map<String, String> personalInfo = new HashMap<>();
        personalInfo.put("name", "");
        personalInfo.put("email", "");
        personalInfo.put("phone", "");
        personalInfo.put("location", "");
        personalInfo.put("linkedin", "");
        personalInfo.put("github", "");
        personalInfo.put("portfolio", "");
        personalInfo.put("website", "");

        if (resumeText != null) {
            String text = resumeText.toLowerCase();
            
            java.util.regex.Matcher emailMatcher = java.util.regex.Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matcher(resumeText);
            if (emailMatcher.find()) {
                personalInfo.put("email", emailMatcher.group());
            }

            java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern.compile("\\+?\\d{3}[\\s.-]?\\d{3}[\\s.-]?\\d{4}|\\+?\\d{10,12}").matcher(resumeText);
            if (phoneMatcher.find()) {
                personalInfo.put("phone", phoneMatcher.group());
            }

            if (text.contains("linkedin.com/")) {
                int startIdx = text.indexOf("linkedin.com/");
                int endIdx = text.indexOf(" ", startIdx);
                if (endIdx == -1) endIdx = text.length();
                personalInfo.put("linkedin", resumeText.substring(startIdx, Math.min(endIdx, startIdx + 50)).trim());
            }
            if (text.contains("github.com/")) {
                int startIdx = text.indexOf("github.com/");
                int endIdx = text.indexOf(" ", startIdx);
                if (endIdx == -1) endIdx = text.length();
                personalInfo.put("github", resumeText.substring(startIdx, Math.min(endIdx, startIdx + 50)).trim());
            }
        }
        out.put("personalInfo", personalInfo);

        Map<String, String> education = new HashMap<>();
        education.put("degree", "");
        education.put("department", "");
        education.put("college", "");
        education.put("university", "");
        education.put("cgpa", "");
        education.put("percentage", "");
        education.put("graduationYear", "");
        out.put("education", education);

        Map<String, List<String>> skills = new HashMap<>();
        skills.put("programmingLanguages", new ArrayList<>());
        skills.put("frameworks", new ArrayList<>());
        skills.put("libraries", new ArrayList<>());
        skills.put("databases", new ArrayList<>());
        skills.put("cloud", new ArrayList<>());
        skills.put("devops", new ArrayList<>());
        skills.put("tools", new ArrayList<>());
        out.put("skills", skills);

        out.put("projects", new ArrayList<>());
        out.put("certifications", new ArrayList<>());
        out.put("experience", new ArrayList<>());
        out.put("languages", new ArrayList<>());

        Map<String, Object> legacyProfile = new HashMap<>();
        legacyProfile.put("name", null);
        legacyProfile.put("email", null);
        legacyProfile.put("phone", null);
        legacyProfile.put("location", null);
        legacyProfile.put("linkedin", null);
        legacyProfile.put("github", null);
        
        Map<String, Object> legacyEdu = new HashMap<>();
        legacyEdu.put("degree", null);
        legacyEdu.put("college", null);
        legacyEdu.put("cgpa", null);
        legacyEdu.put("graduationYear", null);
        legacyProfile.put("education", legacyEdu);

        Map<String, Object> legacySkillsMap = new HashMap<>();
        legacySkillsMap.put("languages", new ArrayList<String>());
        legacySkillsMap.put("frameworks", new ArrayList<String>());
        legacySkillsMap.put("databases", new ArrayList<String>());
        legacySkillsMap.put("tools", new ArrayList<String>());
        legacyProfile.put("skills", legacySkillsMap);

        legacyProfile.put("projects", new ArrayList<String>());
        legacyProfile.put("certifications", new ArrayList<String>());
        legacyProfile.put("experience", new ArrayList<String>());
        legacyProfile.put("languagesKnown", new ArrayList<String>());

        if (personalInfo.get("email") != null && !personalInfo.get("email").isEmpty()) {
            legacyProfile.put("email", personalInfo.get("email"));
        }
        if (personalInfo.get("phone") != null && !personalInfo.get("phone").isEmpty()) {
            legacyProfile.put("phone", personalInfo.get("phone"));
        }
        if (personalInfo.get("linkedin") != null && !personalInfo.get("linkedin").isEmpty()) {
            legacyProfile.put("linkedin", personalInfo.get("linkedin"));
        }
        if (personalInfo.get("github") != null && !personalInfo.get("github").isEmpty()) {
            legacyProfile.put("github", personalInfo.get("github"));
        }

        out.put("profile", legacyProfile);

        String summary = generateProfessionalSummary(legacyProfile, resumeText);
        out.put("summary", summary);

        return out;
    }

    public static String generateProfessionalSummary(Map<String, Object> profile, String resumeText) {
        Map<String, Object> eduMap = profile.get("education") instanceof Map ? (Map<String, Object>) profile.get("education") : Map.of();
        String degree = eduMap.get("degree") instanceof String ? (String) eduMap.get("degree") : null;
        if (degree == null || degree.isEmpty()) {
            degree = "Computer Science and Business Systems";
        }
        String college = eduMap.get("college") instanceof String ? (String) eduMap.get("college") : null;
        String collegeStr = (college != null && !college.isEmpty()) ? " at " + college : "";
        
        List<String> skills = new ArrayList<>();
        Map<String, Object> skMap = profile.get("skills") instanceof Map ? (Map<String, Object>) profile.get("skills") : Map.of();
        if (skMap.get("languages") instanceof List) {
            for (Object o : (List<?>) skMap.get("languages")) skills.add(String.valueOf(o));
        }
        if (skMap.get("frameworks") instanceof List) {
            for (Object o : (List<?>) skMap.get("frameworks")) skills.add(String.valueOf(o));
        }
        if (skMap.get("databases") instanceof List) {
            for (Object o : (List<?>) skMap.get("databases")) skills.add(String.valueOf(o));
        }
        if (skMap.get("tools") instanceof List) {
            for (Object o : (List<?>) skMap.get("tools")) skills.add(String.valueOf(o));
        }
        
        String skillsStr = skills.isEmpty() ? "software development technologies" : String.join(", ", skills.subList(0, Math.min(skills.size(), 5)));
        
        List<String> projects = profile.get("projects") instanceof List ? (List<String>) profile.get("projects") : List.of();
        String projectsStr = (projects != null && !projects.isEmpty()) ? " including " + String.join(" and ", projects.subList(0, Math.min(projects.size(), 2))) : "";
        
        String achievementsStr = "";
        if (resumeText != null) {
            String textLower = resumeText.toLowerCase();
            if (textLower.contains("leetcode") || textLower.contains("codechef")) {
                achievementsStr = " Experienced in problem solving with coding challenges solved across LeetCode and CodeChef.";
            }
        }
        
        return String.format("%s student%s with strong knowledge of %s. Developed projects%s.%s Seeking Software Developer opportunities to build scalable applications and contribute to modern software engineering teams.",
            degree, collegeStr, skillsStr, projectsStr, achievementsStr);
    }
}
