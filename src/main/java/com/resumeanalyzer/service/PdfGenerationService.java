package com.resumeanalyzer.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.resumeanalyzer.dto.*;

@Service
public class PdfGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);

    public byte[] generatePdf(ResumeAnalysisResponse data) {
        log.info("Generating PDF report in the backend...");
        try (PDDocument doc = new PDDocument()) {
            PdfContext context = new PdfContext(doc);
            context.generate(data);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private static class PdfContext {
        private final PDDocument document;
        private PDPageContentStream currentStream;
        private float yPosition;
        private final float margin = 50;
        private final float width = 512; // 612 - 2 * 50

        public PdfContext(PDDocument document) throws IOException {
            this.document = document;
            addNewPage();
        }

        private void addNewPage() throws IOException {
            if (currentStream != null) {
                currentStream.close();
            }
            PDPage page = new PDPage();
            document.addPage(page);
            currentStream = new PDPageContentStream(document, page);
            yPosition = 750; // Set starting Y coordinate
        }

        private void checkPageBreak(float requiredHeight) throws IOException {
            if (yPosition - requiredHeight < margin) {
                addNewPage();
            }
        }

        public void generate(ResumeAnalysisResponse data) throws IOException {
            // Title Header
            checkPageBreak(50);
            currentStream.setNonStrokingColor(21, 101, 192); // Nice primary blue
            currentStream.addRect(margin, yPosition - 15, width, 30);
            currentStream.fill();
            
            currentStream.setNonStrokingColor(255, 255, 255);
            writeText("AI RESUME ANALYSIS REPORT", PDType1Font.HELVETICA_BOLD, 14, margin + 10);
            yPosition -= 35;

            // Personal Info
            currentStream.setNonStrokingColor(33, 33, 33);
            if (data.personalInfo() != null) {
                checkPageBreak(50);
                writeText(sanitizeText(data.personalInfo().name()), PDType1Font.HELVETICA_BOLD, 12, margin);
                yPosition -= 14;
                
                String contactStr = "";
                if (data.personalInfo().email() != null && !data.personalInfo().email().isEmpty()) {
                    contactStr += "Email: " + data.personalInfo().email();
                }
                if (data.personalInfo().phone() != null && !data.personalInfo().phone().isEmpty()) {
                    if (!contactStr.isEmpty()) contactStr += " | ";
                    contactStr += "Phone: " + data.personalInfo().phone();
                }
                if (data.personalInfo().location() != null && !data.personalInfo().location().isEmpty()) {
                    if (!contactStr.isEmpty()) contactStr += " | ";
                    contactStr += "Loc: " + data.personalInfo().location();
                }
                writeText(sanitizeText(contactStr), PDType1Font.HELVETICA, 9, margin);
                yPosition -= 12;

                String linksStr = "";
                if (data.personalInfo().linkedin() != null && !data.personalInfo().linkedin().isEmpty()) {
                    linksStr += "LinkedIn: " + data.personalInfo().linkedin();
                }
                if (data.personalInfo().github() != null && !data.personalInfo().github().isEmpty()) {
                    if (!linksStr.isEmpty()) linksStr += " | ";
                    linksStr += "GitHub: " + data.personalInfo().github();
                }
                if (!linksStr.isEmpty()) {
                    writeText(sanitizeText(linksStr), PDType1Font.HELVETICA, 9, margin);
                    yPosition -= 12;
                }
            }
            yPosition -= 10;

            // ATS Score Box
            checkPageBreak(35);
            currentStream.setNonStrokingColor(240, 244, 248); // light blue-gray background
            currentStream.addRect(margin, yPosition - 5, width, 25);
            currentStream.fill();
            
            currentStream.setNonStrokingColor(21, 101, 192);
            writeText("OVERALL ATS SCORE: " + data.atsScore() + "/100", PDType1Font.HELVETICA_BOLD, 12, margin + 10);
            yPosition -= 20;

            // Professional Summary
            if (data.summary() != null && !data.summary().trim().isEmpty()) {
                drawSectionHeader("PROFESSIONAL SUMMARY");
                currentStream.setNonStrokingColor(33, 33, 33);
                List<String> summaryLines = wrapText(sanitizeText(data.summary()), PDType1Font.HELVETICA, 9, width - 10);
                for (String line : summaryLines) {
                    checkPageBreak(12);
                    writeText(line, PDType1Font.HELVETICA, 9, margin + 5);
                    yPosition -= 12;
                }
                yPosition -= 5;
            }

            // Skills Section
            if ((data.matchedSkills() != null && !data.matchedSkills().isEmpty()) || 
                (data.missingSkills() != null && !data.missingSkills().isEmpty())) {
                drawSectionHeader("SKILLS ANALYSIS");
                currentStream.setNonStrokingColor(33, 33, 33);

                if (data.matchedSkills() != null && !data.matchedSkills().isEmpty()) {
                    checkPageBreak(15);
                    writeText("Matched Skills:", PDType1Font.HELVETICA_BOLD, 9, margin + 5);
                    yPosition -= 11;
                    String matchedStr = String.join(", ", data.matchedSkills());
                    List<String> matchedLines = wrapText(sanitizeText(matchedStr), PDType1Font.HELVETICA, 9, width - 10);
                    for (String line : matchedLines) {
                        checkPageBreak(11);
                        writeText(line, PDType1Font.HELVETICA, 9, margin + 15);
                        yPosition -= 11;
                    }
                    yPosition -= 5;
                }

                if (data.missingSkills() != null && !data.missingSkills().isEmpty()) {
                    checkPageBreak(15);
                    writeText("Missing Skills (Recommended to add):", PDType1Font.HELVETICA_BOLD, 9, margin + 5);
                    yPosition -= 11;
                    String missingStr = String.join(", ", data.missingSkills());
                    List<String> missingLines = wrapText(sanitizeText(missingStr), PDType1Font.HELVETICA, 9, width - 10);
                    for (String line : missingLines) {
                        checkPageBreak(11);
                        writeText(line, PDType1Font.HELVETICA, 9, margin + 15);
                        yPosition -= 11;
                    }
                    yPosition -= 5;
                }
            }

            // Experience Section
            if (data.experience() != null && !data.experience().isEmpty()) {
                drawSectionHeader("WORK EXPERIENCE");
                currentStream.setNonStrokingColor(33, 33, 33);
                for (ExperienceDetail exp : data.experience()) {
                    checkPageBreak(25);
                    String roleCompany = exp.role();
                    if (exp.company() != null && !exp.company().isEmpty()) {
                        roleCompany += " at " + exp.company();
                    }
                    if (exp.duration() != null && !exp.duration().isEmpty()) {
                        roleCompany += " (" + exp.duration() + ")";
                    }
                    writeText(sanitizeText(roleCompany), PDType1Font.HELVETICA_BOLD, 9, margin + 5);
                    yPosition -= 11;

                    if (exp.responsibilities() != null) {
                        for (String resp : exp.responsibilities()) {
                            List<String> wrappedResp = wrapText(sanitizeText(resp), PDType1Font.HELVETICA, 8.5f, width - 25);
                            for (String line : wrappedResp) {
                                checkPageBreak(11);
                                writeText("- " + line, PDType1Font.HELVETICA, 8.5f, margin + 15);
                                yPosition -= 11;
                            }
                        }
                    }
                    yPosition -= 5;
                }
            }

            // Projects Section
            if (data.projects() != null && !data.projects().isEmpty()) {
                drawSectionHeader("PROJECTS");
                currentStream.setNonStrokingColor(33, 33, 33);
                for (ProjectDetail proj : data.projects()) {
                    checkPageBreak(25);
                    String titleStr = proj.projectName();
                    if (proj.role() != null && !proj.role().isEmpty()) {
                        titleStr += " - Role: " + proj.role();
                    }
                    writeText(sanitizeText(titleStr), PDType1Font.HELVETICA_BOLD, 9, margin + 5);
                    yPosition -= 11;

                    if (proj.description() != null && !proj.description().isEmpty()) {
                        List<String> wrappedDesc = wrapText(sanitizeText(proj.description()), PDType1Font.HELVETICA, 8.5f, width - 15);
                        for (String line : wrappedDesc) {
                            checkPageBreak(11);
                            writeText(line, PDType1Font.HELVETICA, 8.5f, margin + 10);
                            yPosition -= 11;
                        }
                    }

                    if (proj.techStack() != null && !proj.techStack().isEmpty()) {
                        checkPageBreak(11);
                        writeText("Tech Stack: " + String.join(", ", proj.techStack()), PDType1Font.HELVETICA, 8.5f, margin + 10);
                        yPosition -= 11;
                    }
                    yPosition -= 5;
                }
            }

            // Education Section
            if (data.education() != null && data.education().degree() != null && !data.education().degree().isEmpty()) {
                drawSectionHeader("EDUCATION");
                currentStream.setNonStrokingColor(33, 33, 33);
                checkPageBreak(25);
                EducationDetail edu = data.education();
                String eduStr = edu.degree();
                if (edu.department() != null && !edu.department().isEmpty()) {
                    eduStr += " in " + edu.department();
                }
                writeText(sanitizeText(eduStr), PDType1Font.HELVETICA_BOLD, 9, margin + 5);
                yPosition -= 11;

                String colStr = "";
                if (edu.college() != null && !edu.college().isEmpty()) {
                    colStr += edu.college();
                }
                if (edu.graduationYear() != null && !edu.graduationYear().isEmpty()) {
                    if (!colStr.isEmpty()) colStr += " (Graduation: " + edu.graduationYear() + ")";
                }
                if (!colStr.isEmpty()) {
                    writeText(sanitizeText(colStr), PDType1Font.HELVETICA, 9, margin + 5);
                    yPosition -= 11;
                }

                if (edu.cgpa() != null && !edu.cgpa().isEmpty()) {
                    writeText("CGPA/Grade: " + sanitizeText(edu.cgpa()), PDType1Font.HELVETICA, 9, margin + 5);
                    yPosition -= 11;
                }
                yPosition -= 5;
            }

            // Certifications
            if (data.certifications() != null && !data.certifications().isEmpty()) {
                drawSectionHeader("CERTIFICATIONS");
                currentStream.setNonStrokingColor(33, 33, 33);
                for (CertificationDetail cert : data.certifications()) {
                    checkPageBreak(12);
                    String certStr = cert.certificationName();
                    if (cert.platform() != null && !cert.platform().isEmpty()) {
                        certStr += " - " + cert.platform();
                    }
                    writeText("- " + sanitizeText(certStr), PDType1Font.HELVETICA, 9, margin + 5);
                    yPosition -= 12;
                }
                yPosition -= 5;
            }

            // Strengths and Weaknesses
            if ((data.strengths() != null && !data.strengths().isEmpty()) || 
                (data.weaknesses() != null && !data.weaknesses().isEmpty())) {
                drawSectionHeader("STRENGTHS & WEAKNESSES");
                currentStream.setNonStrokingColor(33, 33, 33);

                if (data.strengths() != null && !data.strengths().isEmpty()) {
                    checkPageBreak(15);
                    writeText("Strengths:", PDType1Font.HELVETICA_BOLD, 9, margin + 5);
                    yPosition -= 11;
                    for (String str : data.strengths()) {
                        List<String> wrappedStr = wrapText(sanitizeText(str), PDType1Font.HELVETICA, 9, width - 20);
                        for (String line : wrappedStr) {
                            checkPageBreak(11);
                            writeText("- " + line, PDType1Font.HELVETICA, 9, margin + 15);
                            yPosition -= 11;
                        }
                    }
                    yPosition -= 5;
                }

                if (data.weaknesses() != null && !data.weaknesses().isEmpty()) {
                    checkPageBreak(15);
                    writeText("Weaknesses & Areas of Improvement:", PDType1Font.HELVETICA_BOLD, 9, margin + 5);
                    yPosition -= 11;
                    for (String weak : data.weaknesses()) {
                        List<String> wrappedWeak = wrapText(sanitizeText(weak), PDType1Font.HELVETICA, 9, width - 20);
                        for (String line : wrappedWeak) {
                            checkPageBreak(11);
                            writeText("- " + line, PDType1Font.HELVETICA, 9, margin + 15);
                            yPosition -= 11;
                        }
                    }
                    yPosition -= 5;
                }
            }

            // Recommendations
            if (data.recommendations() != null && !data.recommendations().isEmpty()) {
                drawSectionHeader("RECOMMENDATIONS");
                currentStream.setNonStrokingColor(33, 33, 33);
                for (RecommendationDetail rec : data.recommendations()) {
                    checkPageBreak(25);
                    writeText(sanitizeText(rec.title()), PDType1Font.HELVETICA_BOLD, 9, margin + 5);
                    yPosition -= 11;
                    List<String> wrappedText = wrapText(sanitizeText(rec.text()), PDType1Font.HELVETICA, 8.5f, width - 15);
                    for (String line : wrappedText) {
                        checkPageBreak(11);
                        writeText(line, PDType1Font.HELVETICA, 8.5f, margin + 10);
                        yPosition -= 11;
                    }
                    yPosition -= 5;
                }
            }

            // Close current stream
            if (currentStream != null) {
                currentStream.close();
            }
        }

        private void drawSectionHeader(String title) throws IOException {
            checkPageBreak(35);
            yPosition -= 12;
            currentStream.setNonStrokingColor(21, 101, 192); // Nice blue
            currentStream.addRect(margin, yPosition - 3, width, 16);
            currentStream.fill();
            
            currentStream.setNonStrokingColor(255, 255, 255);
            writeText(title, PDType1Font.HELVETICA_BOLD, 10, margin + 5);
            yPosition -= 12;
        }

        private void writeText(String text, PDFont font, float fontSize, float xOffset) throws IOException {
            currentStream.beginText();
            currentStream.setFont(font, fontSize);
            currentStream.newLineAtOffset(xOffset, yPosition);
            currentStream.showText(text);
            currentStream.endText();
        }

        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> result = new ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float width = font.getStringWidth(testLine) / 1000 * fontSize;
                if (width > maxWidth) {
                    if (currentLine.length() > 0) {
                        result.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        result.add(word);
                    }
                } else {
                    currentLine.append(currentLine.length() == 0 ? "" : " ").append(word);
                }
            }
            if (currentLine.length() > 0) {
                result.add(currentLine.toString());
            }
            return result;
        }

        private String sanitizeText(String text) {
            if (text == null) return "";
            return text.replace("•", "-")
                       .replace("\u2022", "-")
                       .replace("\u201c", "\"")
                       .replace("\u201d", "\"")
                       .replace("\u2018", "'")
                       .replace("\u2019", "'")
                       .replace("\u2014", "-")
                       .replaceAll("[^\\x20-\\x7E]", "");
        }
    }
}
