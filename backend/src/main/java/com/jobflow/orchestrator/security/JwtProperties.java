package com.jobflow.orchestrator.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobflow.security")
public class JwtProperties {

    private String jwtSecret = "change-me-in-production-min-32-chars-long";
    private long jwtExpirationMinutes = 60;

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public long getJwtExpirationMinutes() { return jwtExpirationMinutes; }
    public void setJwtExpirationMinutes(long jwtExpirationMinutes) { this.jwtExpirationMinutes = jwtExpirationMinutes; }
}
