package com.promptform.api.auth;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.promptform.api.ratelimit.RateLimitException;
import com.promptform.api.ratelimit.RateLimiter;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseCookie;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RateLimiter rateLimiter;

    public AuthController(AuthService authService, RateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthModels.AuthResponse register(@Valid @RequestBody AuthModels.RegisterRequest request,
                                            HttpServletRequest httpRequest, HttpServletResponse response) {
        checkRateLimit(httpRequest);
        AuthModels.AuthResponse authResponse = authService.register(request);
        writeAccessCookie(response, authResponse.accessToken());
        return authResponse;
    }

    @PostMapping("/login")
    public AuthModels.AuthResponse login(@Valid @RequestBody AuthModels.LoginRequest request,
                                         HttpServletRequest httpRequest, HttpServletResponse response) {
        checkRateLimit(httpRequest);
        AuthModels.AuthResponse authResponse = authService.login(request);
        writeAccessCookie(response, authResponse.accessToken());
        return authResponse;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", accessCookie("", 0).toString());
    }

    @GetMapping("/me")
    public AuthModels.UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return authService.getUser(UUID.fromString(jwt.getSubject()));
    }

    private void checkRateLimit(HttpServletRequest request) {
        if (!rateLimiter.allow("auth:" + request.getRemoteAddr())) {
            throw new RateLimitException("Too many authentication attempts. Try again in a minute.");
        }
    }

    private void writeAccessCookie(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", accessCookie(token, authService.accessTokenLifetimeSeconds()).toString());
    }

    private ResponseCookie accessCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from("promptform_access_token", value)
                .httpOnly(true)
                .secure(authService.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}
