package com.promptform.api.auth;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
