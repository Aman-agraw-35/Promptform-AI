package com.promptform.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@TestPropertySource(properties = {
        "JWT_SECRET=local-context-test-secret-that-is-long-enough-123456",
        "PROMPTFORM_LLM_PROVIDER=demo",
        "spring.datasource.url=jdbc:h2:mem:promptform;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ApplicationContextTest {
    @Test
    void applicationContextStartsWithRedisDisabled() {
        assertThatCode(() -> { }).doesNotThrowAnyException();
    }
}
