package com.resumeanalyzer.service;

import java.io.InputStream;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.resumeanalyzer.exception.FileProcessingException;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);
    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            log.info("Extracting text using Apache Tika from file: {}, size: {}", 
                     file.getOriginalFilename(), file.getSize());
            String text = tika.parseToString(in);
            if (text == null) {
                return "";
            }
            return text;
        } catch (Exception e) {
            log.error("Failed to extract text from resume using Apache Tika", e);
            throw new FileProcessingException("Failed to extract text from resume using Apache Tika", e);
        }
    }
}
