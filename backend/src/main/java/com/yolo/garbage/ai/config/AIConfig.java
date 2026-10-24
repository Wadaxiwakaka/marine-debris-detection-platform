package com.yolo.garbage.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Value("${ai.deepseek.api-key}")
    private String deepSeekApiKey;

    @Value("${ai.glm.api-key}")
    private String glmApiKey;

    @Value("${ai.system-prompt.file}")
    private String systemPromptFile;

    public String getDeepSeekApiKey() {
        return deepSeekApiKey;
    }

    public String getGlmApiKey() {
        return glmApiKey;
    }

    public String getSystemPromptFile() {
        return systemPromptFile;
    }
}