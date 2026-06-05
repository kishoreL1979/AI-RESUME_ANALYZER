package com.resumeanalyzer.integration;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.resumeanalyzer.dto.ResumeAnalysisResponse;
import com.resumeanalyzer.service.ATSService;
import com.resumeanalyzer.service.SkillMatchingService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AnalyzeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private SkillMatchingService skillMatchingService;

    @Autowired
    private ATSService atsService;

    @Test
    public void testPdfUploadAndAnalyze() throws Exception {
        File pdf = createSamplePdf("Java developer with Spring Boot and AWS experience. 5 years experience.");

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new FileSystemResource(pdf));
        parts.add("jobDescription", "Java Spring Boot AWS");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(parts, headers);

        RestTemplate rest = new RestTemplate();
        String url = "http://localhost:" + port + "/api/analyze";
        ResponseEntity<ResumeAnalysisResponse> resp = rest.postForEntity(url, request, ResumeAnalysisResponse.class);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        ResumeAnalysisResponse body = resp.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.atsScore() >= 0 && body.atsScore() <= 100);
        Assertions.assertNotNull(body.matchedSkills());

        Files.deleteIfExists(pdf.toPath());
    }

    @Test
    public void testDocxUploadAndAnalyze() throws Exception {
        File docx = createSampleDocx("Experienced Java engineer with SQL and Docker knowledge. 3 years experience.");

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new FileSystemResource(docx));
        parts.add("jobDescription", "Java SQL Docker");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(parts, headers);

        RestTemplate rest = new RestTemplate();
        String url = "http://localhost:" + port + "/api/analyze";
        ResponseEntity<ResumeAnalysisResponse> resp = rest.postForEntity(url, request, ResumeAnalysisResponse.class);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        ResumeAnalysisResponse body = resp.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.atsScore() >= 0 && body.atsScore() <= 100);
        Assertions.assertNotNull(body.matchedSkills());

        Files.deleteIfExists(docx.toPath());
    }

    @Test
    public void testSkillMatchingAndAts() {
        String resume = "Java Spring Boot AWS Docker 6 years";
        String job = "Java Spring Boot AWS Kubernetes";

        var skillResult = skillMatchingService.match(resume, job);
        Assertions.assertTrue(skillResult.getMatchedSkills().contains("Java"));

        var ats = atsService.calculate(resume, job, skillResult);
        Assertions.assertTrue(ats.getScore() >= 0 && ats.getScore() <= 100);
    }

    private File createSamplePdf(String text) throws Exception {
        File tmp = File.createTempFile("sample", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            doc.save(tmp);
        }
        return tmp;
    }

    private File createSampleDocx(String text) throws Exception {
        File tmp = File.createTempFile("sample", ".docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText(text);
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                doc.write(out);
            }
        }
        return tmp;
    }
}
