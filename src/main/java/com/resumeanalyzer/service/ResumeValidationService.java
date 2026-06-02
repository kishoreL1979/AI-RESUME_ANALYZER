package com.resumeanalyzer.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.resumeanalyzer.dto.*;

@Service
public class ResumeValidationService {

    public ParsedProfile validate(ParsedProfile profile, String resumeText) {
        if (profile == null) return ParsedProfile.createEmpty();
        String text = resumeText == null ? "" : resumeText.toLowerCase();

        PersonalInfo pi = profile.personalInfo();
        PersonalInfo validatedPi;
        if (pi != null) {
            String name = validateField(pi.name(), text);
            String email = validateField(pi.email(), text);
            String phone = validateField(pi.phone(), text);
            String location = validateField(pi.location(), text);
            String linkedin = validateField(pi.linkedin(), text);
            String github = validateField(pi.github(), text);
            String portfolio = validateField(pi.portfolio(), text);
            String website = validateField(pi.website(), text);
            validatedPi = new PersonalInfo(
                name == null ? "" : name,
                email == null ? "" : email,
                phone == null ? "" : phone,
                location == null ? "" : location,
                linkedin == null ? "" : linkedin,
                github == null ? "" : github,
                portfolio == null ? "" : portfolio,
                website == null ? "" : website
            );
        } else {
            validatedPi = PersonalInfo.createEmpty();
        }

        EducationDetail edu = profile.education();
        EducationDetail validatedEdu;
        if (edu != null) {
            String degree = validateField(edu.degree(), text);
            String department = validateField(edu.department(), text);
            String college = validateField(edu.college(), text);
            String university = validateField(edu.university(), text);
            String cgpa = validateField(edu.cgpa(), text);
            String percentage = validateField(edu.percentage(), text);
            String gradYear = validateField(edu.graduationYear(), text);
            validatedEdu = new EducationDetail(
                degree == null ? "" : degree,
                department == null ? "" : department,
                college == null ? "" : college,
                university == null ? "" : university,
                cgpa == null ? "" : cgpa,
                percentage == null ? "" : percentage,
                gradYear == null ? "" : gradYear
            );
        } else {
            validatedEdu = EducationDetail.createEmpty();
        }

        SkillsDetail sk = profile.skills();
        SkillsDetail validatedSkills;
        if (sk != null) {
            List<String> programmingLanguages = validateList(sk.programmingLanguages(), text);
            List<String> frameworks = validateList(sk.frameworks(), text);
            List<String> libraries = validateList(sk.libraries(), text);
            List<String> databases = validateList(sk.databases(), text);
            List<String> cloud = validateList(sk.cloud(), text);
            List<String> devops = validateList(sk.devops(), text);
            List<String> tools = validateList(sk.tools(), text);
            validatedSkills = new SkillsDetail(
                programmingLanguages, frameworks, libraries, databases, cloud, devops, tools
            );
        } else {
            validatedSkills = SkillsDetail.createEmpty();
        }

        List<ProjectDetail> projects = validateProjects(profile.projects(), text);
        List<CertificationDetail> certifications = validateCertifications(profile.certifications(), text);
        List<ExperienceDetail> experience = validateExperience(profile.experience(), text);
        List<String> languages = validateList(profile.languages(), text);

        return new ParsedProfile(
            validatedPi, validatedEdu, validatedSkills, projects, certifications, experience, languages
        );
    }

    private String validateField(String field, String resumeText) {
        if (field == null || field.trim().isEmpty() || "null".equalsIgnoreCase(field) || "Not Available".equalsIgnoreCase(field)) {
            return null;
        }
        
        String cleanVal = field.toLowerCase().trim();
        
        if (resumeText.contains(cleanVal)) {
            return field;
        }
        
        String[] parts = cleanVal.split("\\s+");
        for (String part : parts) {
            if (part.length() > 3 && resumeText.contains(part)) {
                return field;
            }
        }
        
        return null;
    }

    private List<String> validateList(List<String> list, String resumeText) {
        if (list == null) return List.of();
        List<String> validList = new ArrayList<>();
        for (String item : list) {
            if (item == null || item.trim().isEmpty()) continue;
            
            String cleanItem = item.toLowerCase().trim();
            if (resumeText.contains(cleanItem)) {
                validList.add(item);
                continue;
            }
            
            String[] words = cleanItem.split("[^a-zA-Z0-9]+");
            int matches = 0;
            int totalCheck = 0;
            for (String word : words) {
                if (word.length() > 3) {
                    totalCheck++;
                    if (resumeText.contains(word)) {
                        matches++;
                    }
                }
            }
            if (totalCheck > 0 && ((double) matches / totalCheck) >= 0.5) {
                validList.add(item);
            }
        }
        return validList;
    }

    private List<ProjectDetail> validateProjects(List<ProjectDetail> list, String resumeText) {
        if (list == null) return List.of();
        List<ProjectDetail> validList = new ArrayList<>();
        for (ProjectDetail item : list) {
            if (item == null) continue;
            String name = validateField(item.projectName(), resumeText);
            String desc = validateField(item.description(), resumeText);
            List<String> tech = validateList(item.techStack(), resumeText);
            String role = validateField(item.role(), resumeText);
            
            if (name != null || desc != null || !tech.isEmpty()) {
                validList.add(new ProjectDetail(
                    name == null ? "" : name,
                    desc == null ? "" : desc,
                    tech,
                    role == null ? "" : role
                ));
            }
        }
        return validList;
    }

    private List<CertificationDetail> validateCertifications(List<CertificationDetail> list, String resumeText) {
        if (list == null) return List.of();
        List<CertificationDetail> validList = new ArrayList<>();
        for (CertificationDetail item : list) {
            if (item == null) continue;
            String name = validateField(item.certificationName(), resumeText);
            String platform = validateField(item.platform(), resumeText);
            
            if (name != null) {
                validList.add(new CertificationDetail(
                    name,
                    platform == null ? "" : platform
                ));
            }
        }
        return validList;
    }

    private List<ExperienceDetail> validateExperience(List<ExperienceDetail> list, String resumeText) {
        if (list == null) return List.of();
        List<ExperienceDetail> validList = new ArrayList<>();
        for (ExperienceDetail item : list) {
            if (item == null) continue;
            String role = validateField(item.role(), resumeText);
            String company = validateField(item.company(), resumeText);
            String duration = validateField(item.duration(), resumeText);
            List<String> resps = validateList(item.responsibilities(), resumeText);
            
            if (role != null || company != null || !resps.isEmpty()) {
                validList.add(new ExperienceDetail(
                    role == null ? "" : role,
                    company == null ? "" : company,
                    duration == null ? "" : duration,
                    resps
                ));
            }
        }
        return validList;
    }
}
