package com.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UrlClick entity — one record per redirect click.
 * Used for analytics: who clicked, when, from where.
 */
@Entity
@Table(name = "url_clicks",
        indexes = {
            @Index(name = "idx_click_short_url", columnList = "shortUrlId"),
            @Index(name = "idx_click_time",      columnList = "clickedAt")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The short URL that was clicked */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shortUrlId", nullable = false)
    private ShortUrl shortUrl;

    /** When the click happened */
    @Builder.Default
    private LocalDateTime clickedAt = LocalDateTime.now();

    /** Visitor's IP address */
    @Column(length = 45)
    private String ipAddress;

    /** Geo-location: country derived from IP */
    @Column(length = 100)
    private String country;

    /** Geo-location: city derived from IP */
    @Column(length = 100)
    private String city;

    /** Device type: DESKTOP / MOBILE / TABLET */
    @Column(length = 20)
    private String deviceType;

    /** Browser name parsed from User-Agent header */
    @Column(length = 100)
    private String browser;

    /** Where the user came from (HTTP Referer header) */
    @Column(length = 512)
    private String referrer;
}
