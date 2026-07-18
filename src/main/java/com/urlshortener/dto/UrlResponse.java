package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Response body for a created/fetched short URL */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponse {

    private Long id;
    private String shortCode;
    private String shortUrl;        // Full short URL e.g. http://localhost:8080/abc123
    private String originalUrl;
    private String title;
    private long clickCount;
    private boolean active;
    private boolean customAlias;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
