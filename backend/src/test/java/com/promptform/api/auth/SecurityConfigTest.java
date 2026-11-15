package com.promptform.api.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    @Test
    void createsAnHs256TokenWithTheConfiguredSecret() {
        String secret = "local-security-test-secret-that-is-long-enough-123456";
        SecurityConfig securityConfig = new SecurityConfig();
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var claims = JwtClaimsSet.builder().subject("user-id").issuedAt(Instant.now()).build();

        String token = securityConfig.jwtEncoder(key)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThat(token).startsWith("eyJhbGciOiJIUzI1NiJ9.");
        assertThat(securityConfig.jwtDecoder(key).decode(token).getSubject()).isEqualTo("user-id");
    }
}
