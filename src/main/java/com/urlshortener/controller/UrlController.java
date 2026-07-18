package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.User;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * URL management controller — requires authentication.
 * POST   /api/urls                → Create short URL
 * GET    /api/urls                → List user's URLs
 * DELETE /api/urls/{shortCode}    → Delete a URL
 * GET    /api/urls/{shortCode}/analytics → Get analytics
 */
@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;

    /** Create a new short URL */
    @PostMapping
    public ResponseEntity<UrlResponse> createUrl(
            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal User user) {

        UrlResponse response = urlShortenerService.createShortUrl(request, user);
        return ResponseEntity.status(201).body(response);
    }

    /** Get all URLs for the logged-in user */
    @GetMapping
    public ResponseEntity<List<UrlResponse>> getUserUrls(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(urlShortenerService.getUserUrls(user));
    }

    /** Soft-delete a short URL */
    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable String shortCode,
            @AuthenticationPrincipal User user) {

        urlShortenerService.deleteUrl(shortCode, user);
        return ResponseEntity.noContent().build();
    }

    /** Get analytics for a short URL */
    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @PathVariable String shortCode,
            @AuthenticationPrincipal User user) {

        ShortUrl shortUrl = urlShortenerService.findByShortCode(shortCode);
        if (shortUrl == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(analyticsService.getAnalytics(shortUrl));
    }
}
