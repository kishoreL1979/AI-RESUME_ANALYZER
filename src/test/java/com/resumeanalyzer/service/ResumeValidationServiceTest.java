package com.resumeanalyzer.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.resumeanalyzer.dto.*;

public class ResumeValidationServiceTest {

    private ResumeValidationService validationService;

    @BeforeEach
    public void setUp() {
        validationService = new ResumeValidationService();
    }

    @Test
    public void testRejectHallucinatedData() {
        String resumeText = "Alex Morgan. Stanford University. Java, Spring Boot, MySQL. Developed E-Commerce Platform.";

        PersonalInfo pi = new PersonalInfo(
            "Alex Morgan",
            "fake-email@stanford.edu", 
            "+1234567890",            
            "San Francisco, CA",      
            "linkedin.com/in/alex",   
            "github.com/alex",        
            "",
            ""
        );

        EducationDetail edu = new EducationDetail("B.S. Computer Science", "", "Stanford University", "", "", "", "");
        
        SkillsDetail sk = new SkillsDetail(
            List.of("Java"),
            List.of("Spring Boot"),
            List.of(),
            List.of("MySQL"),
            List.of(),
            List.of(),
            List.of("Docker")
        );

        List<ProjectDetail> proj = List.of(
            new ProjectDetail("E-Commerce Platform", "", List.of(), ""),
            new ProjectDetail("AI Chatbot", "", List.of(), "")
        );

        List<CertificationDetail> cert = List.of(
            new CertificationDetail("AWS Certified Practitioner", "")
        );

        List<ExperienceDetail> exp = List.of(
            new ExperienceDetail("Software Engineer", "Google", "", List.of())
        );

        ParsedProfile extractedProfile = new ParsedProfile(
            pi, edu, sk, proj, cert, exp, List.of("English", "Spanish")
        );

        ParsedProfile validated = validationService.validate(extractedProfile, resumeText);

        // Name and Education are valid:
        Assertions.assertEquals("Alex Morgan", validated.personalInfo().name());
        Assertions.assertEquals("Stanford University", validated.education().college());

        // Hallucinated fields should be discarded (set to empty string)
        Assertions.assertEquals("", validated.personalInfo().email());
        Assertions.assertEquals("", validated.personalInfo().phone());
        Assertions.assertEquals("", validated.personalInfo().location());
        Assertions.assertEquals("", validated.personalInfo().linkedin());
        Assertions.assertEquals("", validated.personalInfo().github());

        // Skills validation: Docker should be discarded
        Assertions.assertTrue(validated.skills().programmingLanguages().contains("Java"));
        Assertions.assertTrue(validated.skills().frameworks().contains("Spring Boot"));
        Assertions.assertTrue(validated.skills().databases().contains("MySQL"));
        Assertions.assertFalse(validated.skills().tools().contains("Docker"));

        // Projects validation: AI Chatbot should be discarded
        Assertions.assertTrue(validated.projects().stream().anyMatch(p -> "E-Commerce Platform".equals(p.projectName())));
        Assertions.assertFalse(validated.projects().stream().anyMatch(p -> "AI Chatbot".equals(p.projectName())));

        // Certifications, Experience, Languages should be empty
        Assertions.assertTrue(validated.certifications().isEmpty());
        Assertions.assertTrue(validated.experience().isEmpty());
        Assertions.assertTrue(validated.languages().isEmpty());
    }

    @Test
    public void testValidFieldRetention() {
        String resumeText = "Email: alex@example.com. Phone: +15551234567. LinkedIn: linkedin.com/in/alex. GitHub: github.com/alex.";

        PersonalInfo pi = new PersonalInfo(
            "",
            "alex@example.com",
            "+15551234567",
            "",
            "linkedin.com/in/alex",
            "github.com/alex",
            "",
            ""
        );

        ParsedProfile extractedProfile = new ParsedProfile(
            pi,
            EducationDetail.createEmpty(),
            SkillsDetail.createEmpty(),
            List.of(), List.of(), List.of(), List.of()
        );

        ParsedProfile validated = validationService.validate(extractedProfile, resumeText);

        Assertions.assertEquals("alex@example.com", validated.personalInfo().email());
        Assertions.assertEquals("+15551234567", validated.personalInfo().phone());
        Assertions.assertEquals("linkedin.com/in/alex", validated.personalInfo().linkedin());
        Assertions.assertEquals("github.com/alex", validated.personalInfo().github());
    }
}
