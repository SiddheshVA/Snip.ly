package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.User;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core URL shortener service.
 *
 * Key operations:
 *  1. createShortUrl() — persist + generate Base62 code
 *  2. getOriginalUrl()  — cache-first lookup (Caffeine)
 *  3. getUserUrls()     — dashboard listing
 *  4. deleteUrl()       — soft delete + cache eviction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Create a new short URL.
     * 1. Save entity with no shortCode yet (gets DB-assigned ID)
     * 2. Encode the ID to Base62 → shortCode
     * 3. Update entity with shortCode
     * 4. Return full response with the short URL
     */
    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request, User user) {

        // Handle custom alias
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            if (urlRepository.existsByShortCode(request.getCustomAlias())) {
                throw new IllegalArgumentException(
                        "Alias '" + request.getCustomAlias() + "' is already taken. Please try another.");
            }
        }

        // Build entity (shortCode assigned after save)
        ShortUrl shortUrl = ShortUrl.builder()
                .originalUrl(request.getOriginalUrl())
                .title(request.getTitle() != null ? request.getTitle() : extractDomain(request.getOriginalUrl()))
                .user(user)
                .customAlias(request.getCustomAlias() != null && !request.getCustomAlias().isBlank())
                .shortCode(request.getCustomAlias() != null && !request.getCustomAlias().isBlank()
                        ? request.getCustomAlias()
                        : "T" + System.nanoTime())
                .expiresAt(request.getExpiryHours() != null
                        ? LocalDateTime.now().plusHours(request.getExpiryHours()) : null)
                .build();

        // First save — gets the DB-generated ID
        ShortUrl saved = urlRepository.save(shortUrl);

        // Generate short code
        String shortCode = (request.getCustomAlias() != null && !request.getCustomAlias().isBlank())
                ? request.getCustomAlias()
                : base62Encoder.encode(saved.getId());

        saved.setShortCode(shortCode);
        saved = urlRepository.save(saved);

        log.info("Created short URL: {} → {}", shortCode, request.getOriginalUrl());
        return toResponse(saved);
    }

    /**
     * Look up the original URL by short code.
     * @Cacheable means: first call hits DB, subsequent calls served from Caffeine RAM cache.
     */
    @Cacheable(value = "shortUrls", key = "#shortCode")
    public String getOriginalUrl(String shortCode) {
        ShortUrl shortUrl = urlRepository.findByShortCodeAndActiveTrue(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortCode));

        if (shortUrl.isExpired()) {
            throw new IllegalStateException("This link has expired");
        }

        return shortUrl.getOriginalUrl();
    }

    /** Get the ShortUrl entity by code (for analytics logging) */
    public ShortUrl findByShortCode(String shortCode) {
        return urlRepository.findByShortCodeAndActiveTrue(shortCode).orElse(null);
    }

    /** Get all active URLs for the authenticated user */
    public List<UrlResponse> getUserUrls(User user) {
        return urlRepository.findByUserAndActiveTrueOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Soft-delete a URL (sets active=false) and evicts from cache */
    @Transactional
    @CacheEvict(value = "shortUrls", key = "#shortCode")
    public void deleteUrl(String shortCode, User user) {
        ShortUrl shortUrl = urlRepository.findByShortCodeAndActiveTrue(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("URL not found"));

        if (shortUrl.getUser() == null || !shortUrl.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You don't have permission to delete this URL");
        }

        shortUrl.setActive(false);
        urlRepository.save(shortUrl);
        log.info("Deleted short URL: {}", shortCode);
    }

    /** Helper: convert entity → DTO response */
    private UrlResponse toResponse(ShortUrl url) {
        return UrlResponse.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .title(url.getTitle())
                .clickCount(url.getClickCount())
                .active(url.isActive())
                .customAlias(url.isCustomAlias())
                .expiresAt(url.getExpiresAt())
                .createdAt(url.getCreatedAt())
                .build();
    }

    /** Extract domain from URL for default title */
    private String extractDomain(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
