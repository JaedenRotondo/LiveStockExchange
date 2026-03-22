package com.crypto.screener.screeningApplication.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public String getId()            { return id; }
    public String getEmail()         { return email; }
    public String getPasswordHash()  { return passwordHash; }
    public String getFullName()      { return fullName; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setEmail(String email)                { this.email = email; }
    public void setPasswordHash(String passwordHash)  { this.passwordHash = passwordHash; }
    public void setFullName(String fullName)          { this.fullName = fullName; }
}