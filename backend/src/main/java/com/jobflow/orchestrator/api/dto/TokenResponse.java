package com.jobflow.orchestrator.api.dto;

public record TokenResponse(String token, String tokenType) {
    public TokenResponse(String token) {
        this(token, "Bearer");
    }
}
