package com.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ShortUrl entity — the core of the URL shortener.
 * Maps a unique short_code (e.g. "abc123") to an original long URL.
 */
@Entity
@Table(name = "short_urls",
        indexes = {
            @Index(name = "idx_short_code", columnList = "shortCode", unique = true),
            @Index(name = "idx_user_id",    columnList = "userId")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The 6-character Base62 code (e.g. "abc123") — globally unique */
    @Column(nullable = false, unique = true, length = 20)
    private String shortCode;

    /** The full original URL (e.g. https://very-long-url.com/path?q=1) */
    @Column(nullable = false, length = 2048)
    private String originalUrl;

    /** Optional user-provided title / description */
    @Column(length = 255)
    private String title;

    /** The user who created this short URL (null = anonymous) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;

    /** Whether this is a custom alias (user-defined) vs auto-generated */
    @Builder.Default
    private boolean customAlias = false;

    /** Optional expiry — null means never expires */
    private LocalDateTime expiresAt;

    /** Soft-delete flag */
    @Builder.Default
    private boolean active = true;

    /** Running total of clicks (updated async) */
    @Builder.Default
    private Long clickCount = 0L;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Checks if this link has expired */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
