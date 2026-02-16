package com.ApexPay.backend.config;

import com.ApexPay.backend.service.ComplianceAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public ComplianceAgent complianceAgent() {
        // Specify the modelName explicitly to match the demo key's requirements
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();

        return AiServices.create(ComplianceAgent.class, model);
    }
}