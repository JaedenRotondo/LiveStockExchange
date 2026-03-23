package com.crypto.screener.screeningApplication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    // ── Register ─────────────────────────────────────────────────
    public static class RegisterRequest {

        @Email(message = "Valid email required")
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        private String fullName;

        public String getEmail()     { return email; }
        public String getPassword()  { return password; }
        public String getFullName()  { return fullName; }
        public void setEmail(String v)    { this.email = v; }
        public void setPassword(String v) { this.password = v; }
        public void setFullName(String v) { this.fullName = v; }
    }

    // ── Login ────────────────────────────────────────────────────
    public static class LoginRequest {

        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail()    { return email; }
        public String getPassword() { return password; }
        public void setEmail(String v)    { this.email = v; }
        public void setPassword(String v) { this.password = v; }
    }

    // ── Response ─────────────────────────────────────────────────
    // Matches JWT payload: sub = userId (UUID String), email
    public static class AuthResponse {

        private String token;
        private String userId;   // UUID String — becomes "sub" in JWT
        private String email;
        private String fullName;

        public AuthResponse(String token, String userId, String email, String fullName) {
            this.token    = token;
            this.userId   = userId;
            this.email    = email;
            this.fullName = fullName;
        }

        public String getToken()    { return token; }
        public String getUserId()   { return userId; }
        public String getEmail()    { return email; }
        public String getFullName() { return fullName; }
    }
}