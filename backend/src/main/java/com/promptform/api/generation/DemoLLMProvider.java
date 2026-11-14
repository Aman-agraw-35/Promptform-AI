package com.promptform.api.generation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "promptform.llm.provider", havingValue = "demo", matchIfMissing = true)
public class DemoLLMProvider implements LLMProvider {
    @Override
    public String generate(String prompt) {
        return """
            {"title":"Insurance claim","description":"Tell us what happened so we can help.","children":[
            {"type":"text","name":"fullName","label":"Full name","required":true},
            {"type":"text","name":"email","label":"Email address","required":true},
            {"type":"select","name":"vehicleType","label":"Vehicle type","required":true,"options":[{"value":"car","label":"Car"},{"value":"bike","label":"Bike"},{"value":"other","label":"Other"}]},
            {"type":"textarea","name":"incidentDescription","label":"What happened?","required":true}]}
            """;
    }
}
