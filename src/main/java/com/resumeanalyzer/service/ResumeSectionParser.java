package com.resumeanalyzer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.resumeanalyzer.dto.CertificationDetail;
import com.resumeanalyzer.dto.EducationDetail;
import com.resumeanalyzer.dto.ExperienceDetail;
import com.resumeanalyzer.dto.ParsedProfile;
import com.resumeanalyzer.dto.PersonalInfo;
import com.resumeanalyzer.dto.ProjectDetail;
import com.resumeanalyzer.dto.SkillsDetail;

public class ResumeSectionParser {

    public static ParsedProfile parse(String text) {
        if (text == null) return ParsedProfile.createEmpty();
        
        String[] lines = text.split("\\r?\\n");
        String name = "";
        String email = "";
        String phone = "";
        String location = "";
        String linkedin = "";
        String github = "";
        String portfolio = "";
        String website = "";
        
        String degree = "";
        String department = "";
        String college = "";
        String university = "";
        String cgpa = "";
        String percentage = "";
        String graduationYear = "";
        
        List<String> projectLines = new ArrayList<>();
        List<String> certificationLines = new ArrayList<>();
        List<String> experienceLines = new ArrayList<>();
        List<String> languages = new ArrayList<>();
        
        String currentSection = null;
        
        // Helper patterns for contact info
        Pattern emailPat = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Pattern phonePat = Pattern.compile("\\+?\\d{3}[\\s.-]?\\d{3}[\\s.-]?\\d{4}|\\+?\\d{10,12}");
        Pattern linkedinPat = Pattern.compile("linkedin\\.com/in/[a-zA-Z0-9_-]+");
        Pattern githubPat = Pattern.compile("github\\.com/[a-zA-Z0-9_-]+");
        Pattern cgpaPat = Pattern.compile("cgpa:\\s*([0-9.]+)|gpa:\\s*([0-9.]+)|\\b([789]\\.[0-9]{2})\\b", Pattern.CASE_INSENSITIVE);
        Pattern percentPat = Pattern.compile("(\\d{2}\\.?\\d{0,2})\\s*%");
        Pattern yearPat = Pattern.compile("\\b(20\\d{2})\\b");

        // Extract emails/phones/links globally
        Matcher emMatcher = emailPat.matcher(text);
        if (emMatcher.find()) email = emMatcher.group();
        
        Matcher phMatcher = phonePat.matcher(text);
        if (phMatcher.find()) phone = phMatcher.group();
        
        Matcher liMatcher = linkedinPat.matcher(text);
        if (liMatcher.find()) linkedin = liMatcher.group();
        
        Matcher ghMatcher = githubPat.matcher(text);
        if (ghMatcher.find()) github = ghMatcher.group();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            
            String lower = line.toLowerCase();
            
            // Personal info labeled extraction
            if (lower.startsWith("name:")) {
                name = line.substring(5).trim();
                currentSection = null;
                continue;
            }
            if (lower.startsWith("location:")) {
                if (lower.length() > 9) location = line.substring(9).trim();
                else if (i + 1 < lines.length) { location = lines[i+1].trim(); i++; }
                currentSection = null;
                continue;
            }
            if (lower.startsWith("linkedin:")) {
                if (lower.length() > 9) linkedin = line.substring(9).trim();
                else if (i + 1 < lines.length) { linkedin = lines[i+1].trim(); i++; }
                currentSection = null;
                continue;
            }
            if (lower.startsWith("github:")) {
                if (lower.length() > 7) github = line.substring(7).trim();
                else if (i + 1 < lines.length) { github = lines[i+1].trim(); i++; }
                currentSection = null;
                continue;
            }
            if (lower.startsWith("portfolio:") || lower.startsWith("website:")) {
                int len = lower.startsWith("portfolio:") ? 10 : 8;
                if (lower.length() > len) {
                    String url = line.substring(len).trim();
                    if (lower.startsWith("portfolio:")) portfolio = url;
                    else website = url;
                } else if (i + 1 < lines.length) {
                    String url = lines[i+1].trim();
                    if (lower.startsWith("portfolio:")) portfolio = url;
                    else website = url;
                    i++;
                }
                currentSection = null;
                continue;
            }
            
            // Education fields labeled extraction
            if (lower.startsWith("degree:")) {
                if (lower.length() > 7) degree = line.substring(7).trim();
                else if (i + 1 < lines.length) { degree = lines[i+1].trim(); i++; }
                currentSection = null;
                continue;
            }
            if (lower.startsWith("college:") || lower.startsWith("university:") || lower.startsWith("school:")) {
                int len = lower.startsWith("college:") ? 8 : (lower.startsWith("university:") ? 11 : 7);
                String schoolVal = "";
                if (lower.length() > len) {
                    schoolVal = line.substring(len).trim();
                } else if (i + 1 < lines.length) {
                    schoolVal = lines[i+1].trim();
                    i++;
                }
                if (lower.contains("university")) university = schoolVal;
                else college = schoolVal;
                currentSection = null;
                continue;
            }
            if (lower.startsWith("cgpa:")) {
                if (lower.length() > 5) cgpa = line.substring(5).trim();
                else if (i + 1 < lines.length) { cgpa = lines[i+1].trim(); i++; }
                currentSection = null;
                continue;
            }
            
            // Section header detection
            if (lower.startsWith("skills") || lower.startsWith("technical skills") || lower.startsWith("skills & technologies")) {
                currentSection = "skills";
                continue;
            }
            if (lower.startsWith("projects")) {
                currentSection = "projects";
                continue;
            }
            if (lower.startsWith("certifications") || lower.startsWith("courses") || lower.startsWith("licenses & certifications")) {
                currentSection = "certifications";
                continue;
            }
            if (lower.startsWith("experience") || lower.startsWith("work experience") || lower.startsWith("internships") || lower.startsWith("professional experience") || lower.contains("internship") || lower.contains("training")) {
                currentSection = "experience";
                continue;
            }
            if (lower.startsWith("languages")) {
                currentSection = "languages";
                continue;
            }
            
            // Handle section data line by line
            if (currentSection != null) {
                switch (currentSection) {
                    case "skills":
                        break;
                        
                    case "projects":
                        projectLines.add(line);
                        break;
                        
                    case "certifications":
                        certificationLines.add(line);
                        break;
                        
                    case "experience":
                        experienceLines.add(line);
                        break;
                        
                    case "languages":
                        if (line.contains(",")) {
                            for (String l : line.split(",")) languages.add(l.trim());
                        } else {
                            languages.add(line);
                        }
                        break;
                }
            } else {
                if (name.isEmpty() && !lower.contains("resume") && !lower.contains("curriculum")) {
                    name = line;
                }
            }
        }
        
        // Secondary regex-based captures on raw text for specific fields
        if (cgpa.isEmpty()) {
            Matcher m = cgpaPat.matcher(text);
            if (m.find()) {
                cgpa = m.group(1) != null ? m.group(1) : (m.group(2) != null ? m.group(2) : m.group(3));
            }
        }
        Matcher pctMatcher = percentPat.matcher(text);
        if (pctMatcher.find()) percentage = pctMatcher.group(1);
        
        Matcher yrMatcher = yearPat.matcher(text);
        if (yrMatcher.find()) graduationYear = yrMatcher.group(1);
        
        // Extract department / college details from degree description
        if (!degree.isEmpty()) {
            String degLower = degree.toLowerCase();
            if (degLower.contains("computer science")) {
                department = "Computer Science";
                if (degLower.contains("business systems")) department = "Computer Science and Business Systems";
            } else if (degLower.contains("information technology")) {
                department = "Information Technology";
            }
        }

        if ((degree.isBlank() || college.isBlank() || cgpa.isBlank() || graduationYear.isBlank())) {
            String[] resumeLines = text.split("\\r?\\n");
            for (int i = 0; i < resumeLines.length; i++) {
                String row = resumeLines[i].trim();
                String lowerRow = row.toLowerCase();
                if (degree.isBlank() && lowerRow.matches(".*\\b(b\\.?tech|b\\.?e|be|bachelor|mba|m\\.?tech|mtech|master|msc|b\\.?sc|bsc|phd)\\b.*")) {
                    degree = row;
                }
                if (college.isBlank() && lowerRow.matches(".*\\b(institute|university|college|school)\\b.*")) {
                    college = row;
                }
                if (cgpa.isBlank()) {
                    Matcher cgpaMatcher = cgpaPat.matcher(row);
                    if (cgpaMatcher.find()) {
                        cgpa = cgpaMatcher.group(1) != null ? cgpaMatcher.group(1) : (cgpaMatcher.group(2) != null ? cgpaMatcher.group(2) : cgpaMatcher.group(3));
                    }
                }
                if (graduationYear.isBlank()) {
                    Matcher yearMatcher = yearPat.matcher(row);
                    if (yearMatcher.find()) {
                        graduationYear = yearMatcher.group(1);
                    }
                }
            }
        }
        
        Set<String> resumeSkills = ResumeSkillExtractor.extractSkillsFromText(text);
        SkillsDetail skillsDetail = ResumeSkillExtractor.classifySkills(resumeSkills);

        List<ProjectDetail> projects = extractProjects(projectLines);
        if (projects.isEmpty()) {
            projects = fallbackProjectExtraction(text);
        }

        List<CertificationDetail> certifications = extractCertifications(certificationLines);
        if (certifications.isEmpty()) {
            certifications = fallbackCertificationExtraction(text);
        }

        List<ExperienceDetail> experience = extractExperience(experienceLines);

        PersonalInfo personalInfo = new PersonalInfo(name, email, phone, location, linkedin, github, portfolio, website);
        EducationDetail educationDetail = new EducationDetail(degree, department, college, university, cgpa, percentage, graduationYear);
        
        return new ParsedProfile(
            personalInfo, educationDetail, skillsDetail, projects, certifications, experience, languages
        );
    }

    private static List<ProjectDetail> extractProjects(List<String> lines) {
        List<ProjectDetail> projects = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            return projects;
        }

        List<List<String>> blocks = new ArrayList<>();
        List<String> currentBlock = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                if (!currentBlock.isEmpty()) {
                    blocks.add(new ArrayList<>(currentBlock));
                    currentBlock.clear();
                }
                continue;
            }
            if ("__skip__".equals(trimmed)) {
                continue;
            }
            currentBlock.add(trimmed);
        }
        if (!currentBlock.isEmpty()) {
            blocks.add(currentBlock);
        }

        for (List<String> block : blocks) {
            String projectName = "";
            StringBuilder description = new StringBuilder();
            StringBuilder blockText = new StringBuilder();
            for (String line : block) {
                String cleaned = line.replaceAll("^[\\-*•\\s]+", "").trim();
                if (cleaned.isEmpty()) continue;

                blockText.append(cleaned).append(" ");
                if (projectName.isEmpty() && !cleaned.toLowerCase().contains("project")) {
                    projectName = cleaned;
                } else {
                    if (description.length() > 0) description.append(" ");
                    description.append(cleaned);
                }
            }
            if (projectName.isEmpty()) {
                String firstLine = block.get(0).replaceAll("^[\\-*•\\s]+", "").trim();
                projectName = firstLine;
            }
            Set<String> techStack = ResumeSkillExtractor.extractSkillsFromText(blockText.toString());
            projects.add(new ProjectDetail(projectName, description.toString().trim(), new ArrayList<>(techStack), ""));
        }
        return projects;
    }

    private static List<ProjectDetail> fallbackProjectExtraction(String text) {
        List<ProjectDetail> projects = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String row = lines[i].trim();
            if (row.toLowerCase().matches(".*\\b(project|application|platform|system|portal|website)\\b.*") && row.length() > 10) {
                String name = row;
                StringBuilder description = new StringBuilder();
                int j = i + 1;
                while (j < lines.length && !lines[j].trim().isEmpty() && !lines[j].toLowerCase().matches(".*\\b(skills|experience|certifications|education|projects|career)\\b.*")) {
                    if (description.length() > 0) description.append(" ");
                    description.append(lines[j].trim());
                    j++;
                }
                Set<String> techStack = ResumeSkillExtractor.extractSkillsFromText(name + " " + description);
                projects.add(new ProjectDetail(name, description.toString().trim(), new ArrayList<>(techStack), ""));
                i = j;
            }
        }
        return projects;
    }

    private static List<CertificationDetail> extractCertifications(List<String> lines) {
        List<CertificationDetail> certifications = new ArrayList<>();
        if (lines != null) {
            for (String raw : lines) {
                if (raw == null || raw.isBlank()) continue;
                String line = raw.replaceAll("^[\\-*•\\s]+", "").trim();
                if (line.isBlank()) continue;
                String lower = line.toLowerCase();
                String platform = "";
                if (lower.contains("nptel")) platform = "NPTEL";
                else if (lower.contains("coursera")) platform = "Coursera";
                else if (lower.contains("udemy")) platform = "Udemy";
                else if (lower.contains("aws")) platform = "AWS";
                else if (lower.contains("oracle")) platform = "Oracle";
                else if (lower.contains("google")) platform = "Google";
                if (line.length() > 5) {
                    certifications.add(new CertificationDetail(line, platform));
                }
            }
        }
        return certifications;
    }

    private static List<CertificationDetail> fallbackCertificationExtraction(String text) {
        List<CertificationDetail> certifications = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            String lower = line.toLowerCase();
            if (lower.contains("certif") || lower.contains("nptel") || lower.contains("coursera") || lower.contains("udemy") || lower.contains("aws certified") || lower.contains("oracle certified") || lower.contains("google certified") || lower.contains("professional certificate") || lower.contains("certificate")) {
                String platform = "";
                if (lower.contains("nptel")) platform = "NPTEL";
                else if (lower.contains("coursera")) platform = "Coursera";
                else if (lower.contains("udemy")) platform = "Udemy";
                else if (lower.contains("aws")) platform = "AWS";
                else if (lower.contains("oracle")) platform = "Oracle";
                else if (lower.contains("google")) platform = "Google";
                certifications.add(new CertificationDetail(line, platform));
            }
        }
        return certifications;
    }

    private static List<ExperienceDetail> extractExperience(List<String> lines) {
        List<ExperienceDetail> experience = new ArrayList<>();
        if (lines != null && !lines.isEmpty()) {
            String role = "";
            String company = "";
            String duration = "";
            List<String> resps = new ArrayList<>();
            for (String raw : lines) {
                if (raw == null || raw.isBlank()) continue;
                String line = raw.replaceAll("^[\\-*•\\s]+", "").trim();
                if (line.isBlank()) continue;
                if (role.isEmpty()) {
                    role = line;
                } else if (company.isEmpty() && line.length() < 60) {
                    company = line;
                } else {
                    if (line.toLowerCase().contains("-")) {
                        duration = line;
                    } else {
                        resps.add(line);
                    }
                }
            }
            if (!role.isBlank() || !company.isBlank() || !resps.isEmpty()) {
                experience.add(new ExperienceDetail(role, company, duration, resps));
            }
        }
        return experience;
    }

}

