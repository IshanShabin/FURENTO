package com.furniture.rental.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "revoked_token")
public class RevokedToken {

    @Id
    @Column(length = 64)
    private String tokenId;

    @Column(nullable = false)
    private Instant expiresAt;

    public RevokedToken() {}

    public RevokedToken(String tokenId, Instant expiresAt) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}