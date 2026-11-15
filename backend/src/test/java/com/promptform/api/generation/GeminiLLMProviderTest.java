package com.promptform.api.generation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class GeminiLLMProviderTest {
    @Test
    void responseSchemaUsesGeminiCompatibleTypeValues() throws Exception {
        GeminiProperties properties = new GeminiProperties();
        properties.setGeminiApiKey("test-key");
        GeminiLLMProvider provider = new GeminiLLMProvider(new ObjectMapper(), properties);

        Method responseSchema = GeminiLLMProvider.class.getDeclaredMethod("responseSchema");
        responseSchema.setAccessible(true);
        JsonNode schema = (JsonNode) responseSchema.invoke(provider);

        assertTrue(schema.path("properties").path("children").path("items").isObject());
        assertFalse(containsArrayType(schema));
    }

    private boolean containsArrayType(JsonNode node) {
        if (node.isObject()) {
            JsonNode type = node.get("type");
            if (type != null && type.isArray()) return true;
            for (JsonNode child : node) {
                if (containsArrayType(child)) return true;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsArrayType(child)) return true;
            }
        }
        return false;
    }
}
