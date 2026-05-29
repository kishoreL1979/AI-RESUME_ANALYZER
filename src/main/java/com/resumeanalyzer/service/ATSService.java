package com.resumeanalyzer.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.resumeanalyzer.dto.ATSResult;
import com.resumeanalyzer.dto.SkillMatchResult;

@Service
public class ATSService {

    private static final Logger log = LoggerFactory.getLogger(ATSService.class);

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the", "and", "a", "an", "to", "of", "for", "in", "on", "with", "at", "by", "from", 
        "up", "about", "into", "over", "after", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "having", "do", "does", "did", "doing", "but", "or", "as", "if", 
        "then", "else", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", 
        "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", 
        "too", "very", "can", "will", "just", "should", "now", "our", "your", "its", "role", "team",
        "we", "you", "he", "she", "they", "us", "who", "whom", "this", "that", "these", "those"
    ));

    private static final List<String> EQUIVALENT_MAJORS = Arrays.asList(
        "computer science", "computer science engineering", "computer science and engineering", 
        "computer science and business systems", "computer science & business systems", 
        "csbs", "information technology", "software engineering", "it", "cse"
    );

    private static final Pattern MS_PATTERN = Pattern.compile("\\b(master|masters|ms|m\\.sc|mtech|mba)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BS_PATTERN = Pattern.compile("\\b(bachelor|bachelors|bs|b\\.sc|btech|ba|b\\.e\\.)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d{7,15}");

    public boolean isFresher(String resumeText) {
        if (resumeText == null) return true;
        String r = resumeText.toLowerCase();
        
        // Fresher indicator terms
        if (r.contains("fresher") || r.contains("internship") || r.contains("intern") || 
            r.contains("student") || r.contains("pursuing") || r.contains("undergraduate") || 
            r.contains("entry level") || r.contains("junior")) {
            return true;
        }

        // Years of experience check
        Pattern p = Pattern.compile("(\\d{1,2})\\+?\\s+years");
        Matcher m = p.matcher(r);
        int maxYears = 0;
        while (m.find()) {
            try {
                maxYears = Math.max(maxYears, Integer.parseInt(m.group(1)));
            } catch (Exception ignored) {}
        }
        return maxYears < 2;
    }

    private double computeExperienceScore(String resumeText, boolean isFresher) {
        if (resumeText == null) return 0.5;
        String r = resumeText.toLowerCase();
        if (!isFresher) {
            int count = 0;
            if (r.contains("experience") || r.contains("employment") || r.contains("work history") || r.contains("professional background")) {
                count += 2;
            }
            if (r.contains("responsibilities") || r.contains("achieved") || r.contains("led")) {
                count += 1;
            }
            if (r.contains("engineer") || r.contains("developer") || r.contains("analyst") || r.contains("manager")) {
                count += 1;
            }
            if (count >= 3) return 1.0;
            if (count >= 1) return 0.7;
            return 0.4;
        } else {
            boolean hasInternship = r.contains("intern") || r.contains("internship") || r.contains("trainee");
            boolean hasProjects = r.contains("project") || r.contains("developed") || r.contains("portfolio");
            if (hasInternship && hasProjects) {
                return 1.0;
            } else if (hasInternship || hasProjects) {
                return 0.85;
            } else {
                return 0.5;
            }
        }
    }

    public ATSResult calculate(String resumeText, String jobDescription, SkillMatchResult skillMatch) {
        boolean fresher = isFresher(resumeText);
        log.info("Calculating ATS Score. Profile Fresher classification: {}", fresher);

        // 1. Skill Match (35% weight)
        int skillMatchPct = calculateWeightedSkillMatch(skillMatch, fresher);
        int skillFactor = (int) Math.round(skillMatchPct * 0.35);

        // 2. Keyword Match (20% weight)
        double keywordCoverage = computeKeywordCoverage(resumeText, jobDescription);
        int keywordFactor = (int) Math.round(keywordCoverage * 20.0);

        // 3. Education Match (15% weight)
        double educationScore = computeEducationScore(resumeText);
        int educationFactor = (int) Math.round(educationScore * 15.0);

        // 4. Projects (10% weight)
        double projectsScore = computeProjectsScore(resumeText);
        int projectsFactor = (int) Math.round(projectsScore * 10.0);

        // 5. Certifications (10% weight)
        double certsScore = computeCertificationsScore(resumeText);
        int certsFactor = (int) Math.round(certsScore * 10.0);

        // 6. Formatting (5% weight)
        double formattingScore = computeFormattingScore(resumeText);
        int formattingFactor = (int) Math.round(formattingScore * 5.0);

        // 7. Experience (5% weight)
        double experienceScore = computeExperienceScore(resumeText, fresher);
        int experienceFactor = (int) Math.round(experienceScore * 5.0);

        int total = skillFactor + keywordFactor + educationFactor + projectsFactor + certsFactor + formattingFactor + experienceFactor;

        Map<String, Integer> breakdown = new HashMap<>();
        breakdown.put("skillMatch", (int) Math.round(skillMatchPct * 0.40));
        breakdown.put("education", (int) Math.round(educationScore * 20.0));
        breakdown.put("completeness", (int) Math.round(formattingScore * 15.0));
        breakdown.put("keywordCoverage", (int) Math.round(keywordCoverage * 15.0));
        breakdown.put("projectsCertifications", (int) Math.round(((projectsScore + certsScore) / 2.0) * 10.0));
        breakdown.put("experience", (int) Math.round(experienceScore * 20.0));

        return new ATSResult(Math.min(100, total), breakdown);
    }

    private int calculateWeightedSkillMatch(SkillMatchResult skillMatch, boolean isFresher) {
        List<String> matched = skillMatch.getMatchedSkills();
        List<String> missing = skillMatch.getMissingSkills();
        
        double matchedWeight = 0;
        double totalWeight = 0;

        for (String skill : matched) {
            double w = getSkillWeight(skill, isFresher);
            matchedWeight += w;
            totalWeight += w;
        }

        for (String skill : missing) {
            double w = getSkillWeight(skill, isFresher);
            totalWeight += w;
        }

        if (totalWeight == 0) return 0;
        return (int) Math.round((matchedWeight * 100.0) / totalWeight);
    }

    private double getSkillWeight(String skill, boolean isFresher) {
        if (!isFresher) return 1.0;
        
        // De-prioritize advanced enterprise skills for fresher/student profiles
        String s = skill.toLowerCase();
        if (s.contains("docker") || s.contains("kubernetes") || s.contains("k8s") || 
            s.contains("aws") || s.contains("amazon web services") || s.contains("azure") || 
            s.contains("gcp") || s.contains("google cloud") || s.contains("microservices") || 
            s.contains("jenkins") || s.contains("ci/cd") || s.contains("cicd") ||
            s.contains("mlops") || s.contains("devops") || s.contains("terraform") || 
            s.contains("ansible")) {
            return 0.2; // penalty should be small
        }
        return 1.0;
    }

    private double computeEducationScore(String resumeText) {
        if (resumeText == null) return 0.5;
        String r = resumeText.toLowerCase();

        boolean hasDegree = BS_PATTERN.matcher(r).find() || MS_PATTERN.matcher(r).find() || 
                            r.contains("b.tech") || r.contains("m.tech") || r.contains("b.e.") || 
                            r.contains("bachelor") || r.contains("master") || r.contains("degree");
                            
        boolean hasValidMajor = false;
        for (String major : EQUIVALENT_MAJORS) {
            if (r.contains(major)) {
                hasValidMajor = true;
                break;
            }
        }

        if (hasDegree && hasValidMajor) {
            return 1.0;
        }
        if (hasDegree || hasValidMajor) {
            return 0.85;
        }
        return 0.5;
    }

    private double computeProjectsScore(String resumeText) {
        if (resumeText == null) return 0.2;
        String r = resumeText.toLowerCase();
        int count = 0;
        
        if (r.contains("project") || r.contains("developed") || r.contains("portfolio")) {
            Pattern p = Pattern.compile("\\bproject(s)?\\b");
            Matcher m = p.matcher(r);
            while (m.find()) count++;
        }

        if (count >= 2 || r.contains("e-kart") || r.contains("todo application") || r.contains("projects:")) {
            return 1.0;
        }
        if (count == 1) {
            return 0.8;
        }
        return 0.2;
    }

    private double computeCertificationsScore(String resumeText) {
        if (resumeText == null) return 0.2;
        String r = resumeText.toLowerCase();
        int count = 0;
        
        if (r.contains("certificat") || r.contains("certification") || r.contains("certified") || r.contains("credential")) {
            count++;
        }
        if (r.contains("nptel") || r.contains("coursera") || r.contains("udemy")) {
            count += 2;
        }

        if (count >= 2 || r.contains("certifications:")) {
            return 1.0;
        }
        if (count == 1) {
            return 0.8;
        }
        return 0.2;
    }

    private double computeKeywordCoverage(String resume, String job) {
        if (job == null || job.trim().isEmpty() || resume == null) return 0.5;
        String[] words = job.toLowerCase().split("[^a-z0-9+#]+");
        Set<String> uniqueKeywords = new HashSet<>();
        for (String w : words) {
            if (w.length() >= 3 && !STOP_WORDS.contains(w)) {
                uniqueKeywords.add(w);
            }
        }
        if (uniqueKeywords.isEmpty()) return 0.5;

        int found = 0;
        String lowerResume = resume.toLowerCase();
        for (String kw : uniqueKeywords) {
            if (lowerResume.contains(kw)) found++;
        }
        return Math.min(1.0, found / (double) uniqueKeywords.size());
    }

    private double computeFormattingScore(String resumeText) {
        if (resumeText == null) return 0.2;
        String r = resumeText.toLowerCase();
        int present = 0;
        
        if (EMAIL_PATTERN.matcher(r).find() || r.contains("@")) present++;
        if (PHONE_PATTERN.matcher(r).find() || r.contains("phone") || r.contains("contact")) present++;
        if (r.contains("education") || r.contains("college") || r.contains("university") || r.contains("institute")) present++;
        if (r.contains("skills") || r.contains("technical")) present++;
        if (r.contains("project") || r.contains("experience") || r.contains("work")) present++;
        
        return present / 5.0;
    }

    public List<Map<String, Object>> calculateKeywordDensities(String resumeText, List<String> matchedSkills) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (matchedSkills == null || matchedSkills.isEmpty() || resumeText == null) {
            return list;
        }
        String r = resumeText.toLowerCase();
        int maxCount = 0;
        
        Map<String, Integer> counts = new HashMap<>();
        for (String skill : matchedSkills) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(skill.toLowerCase()) + "\\b");
            Matcher m = p.matcher(r);
            int count = 0;
            while (m.find()) {
                count++;
            }
            if (count == 0) {
                int index = 0;
                while ((index = r.indexOf(skill.toLowerCase(), index)) != -1) {
                    count++;
                    index += skill.length();
                }
            }
            counts.put(skill, count);
            maxCount = Math.max(maxCount, count);
        }
        
        for (String skill : matchedSkills) {
            int count = counts.getOrDefault(skill, 0);
            int density = 0;
            if (maxCount > 0) {
                density = (count * 100) / maxCount;
            } else {
                density = 50;
            }
            density = Math.max(15, Math.min(100, density));
            
            Map<String, Object> entry = new HashMap<>();
            entry.put("keyword", skill);
            entry.put("count", count == 0 ? 1 : count);
            entry.put("density", density);
            list.add(entry);
        }
        
        list.sort((a, b) -> Integer.compare((Integer) b.get("count"), (Integer) a.get("count")));
        return list;
    }

    public Map<String, Map<String, Object>> calculateKeywordDensitiesMap(String resumeText, List<String> matchedSkills) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        if (matchedSkills == null) return map;
        
        List<Map<String, Object>> list = calculateKeywordDensities(resumeText, matchedSkills);
        for (Map<String, Object> item : list) {
            String kw = (String) item.get("keyword");
            Map<String, Object> val = new HashMap<>();
            val.put("count", item.get("count"));
            val.put("density", item.get("density"));
            int d = (Integer) item.get("density");
            val.put("status", d >= 70 ? "Optimal" : d >= 40 ? "Moderate" : "Low");
            map.put(kw, val);
        }
        return map;
    }
}
