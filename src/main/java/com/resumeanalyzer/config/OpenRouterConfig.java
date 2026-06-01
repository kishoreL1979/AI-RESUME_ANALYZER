package com.resumeanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenRouterConfig {

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.base-url}")
    private String baseUrl;

    @Value("${openrouter.model:google/gemini-2.5-flash}")
    private String model;

    @Value("${openrouter.enabled:false}")
    private boolean enabled;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
