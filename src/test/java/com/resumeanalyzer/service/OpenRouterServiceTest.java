package com.resumeanalyzer.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.config.OpenRouterConfig;

public class OpenRouterServiceTest {

    private OpenRouterService openRouterService;
    private OpenRouterConfig config;
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        config = new OpenRouterConfig();
        config.setEnabled(false); // disable actual HTTP calls
        config.setApiKey("test-key");
        config.setModel("test-model");
        config.setBaseUrl("http://test-url");
        
        restTemplate = Mockito.mock(RestTemplate.class);
        openRouterService = new OpenRouterService(restTemplate, config);
    }

    @Test
    public void testFallbackResponseStructure() {
        String resumeText = "Java developer. Alex Morgan.";
        Map<String, Object> analysis = openRouterService.fallback(resumeText, List.of("Java"), List.of("AWS"));

        Assertions.assertNotNull(analysis);
        Assertions.assertTrue(analysis.containsKey("summary"));
        Assertions.assertTrue(analysis.containsKey("strengths"));
        Assertions.assertTrue(analysis.containsKey("weaknesses"));
        Assertions.assertTrue(analysis.containsKey("recommendations"));
        Assertions.assertTrue(analysis.containsKey("profile"));

        // Verify that profile fields are null or empty as expected to avoid hallucinations
        Map<String, Object> profile = (Map<String, Object>) analysis.get("profile");
        Assertions.assertNotNull(profile);
        Assertions.assertNull(profile.get("name"));
        Assertions.assertNull(profile.get("email"));
        Assertions.assertNull(profile.get("phone"));
        Assertions.assertNull(profile.get("location"));
    }

    @Test
    public void testJsonExtractionAndParsing() throws Exception {
        // We will test the JSON extractor logic directly or indirectly.
        // Let's verify that a valid JSON inside markdown wrappers can be successfully parsed.
        String jsonWithMarkdown = "```json\n" +
                "{\n" +
                "  \"summary\": \"A short professional summary.\",\n" +
                "  \"strengths\": [\"Java\"],\n" +
                "  \"weaknesses\": [\"AWS\"],\n" +
                "  \"recommendations\": [],\n" +
                "  \"profile\": {\n" +
                "    \"name\": \"Alex\",\n" +
                "    \"email\": \"alex@example.com\",\n" +
                "    \"phone\": \"12345\",\n" +
                "    \"location\": \"SF\",\n" +
                "    \"linkedin\": null,\n" +
                "    \"github\": null,\n" +
                "    \"education\": \"Stanford\",\n" +
                "    \"skills\": [\"Java\"],\n" +
                "    \"projects\": [],\n" +
                "    \"certifications\": [],\n" +
                "    \"experience\": [],\n" +
                "    \"languages\": []\n" +
                "  }\n" +
                "}\n" +
                "```";

        // Enable config to trigger HTTP path mock
        config.setEnabled(true);
        
        String mockResponse = "{\n" +
                "  \"choices\": [{\n" +
                "    \"message\": {\n" +
                "      \"role\": \"assistant\",\n" +
                "      \"content\": " + new ObjectMapper().writeValueAsString(jsonWithMarkdown) + "\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        Mockito.when(restTemplate.postForObject(Mockito.anyString(), Mockito.any(), Mockito.eq(String.class)))
               .thenReturn(mockResponse);

        Map<String, Object> result = openRouterService.generateAiAnalysis("Alex Stanford Java", 80, 80, List.of("Java"), List.of());

        Assertions.assertNotNull(result);
        Assertions.assertEquals("A short professional summary.", result.get("summary"));
        
        Map<String, Object> profile = (Map<String, Object>) result.get("profile");
        Assertions.assertNotNull(profile);
        Assertions.assertEquals("Alex", profile.get("name"));
        Assertions.assertEquals("alex@example.com", profile.get("email"));
    }
}
