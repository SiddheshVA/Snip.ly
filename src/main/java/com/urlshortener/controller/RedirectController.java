package com.urlshortener.controller;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Redirect controller — the PUBLIC endpoint.
 * GET /{shortCode} → 302 redirect to original URL
 *
 * This is THE most critical endpoint — handles every single click.
 * The Caffeine cache ensures it's served from RAM in ~1ms.
 *
 * After redirect: analytics are logged ASYNCHRONOUSLY (non-blocking).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RedirectController {

    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]{3,20}}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        try {
            // 1. Look up original URL (cache-first via @Cacheable in service)
            String originalUrl = urlShortenerService.getOriginalUrl(shortCode);

            // 2. Log click asynchronously (non-blocking — runs in background thread)
            ShortUrl shortUrl = urlShortenerService.findByShortCode(shortCode);
            if (shortUrl != null) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                String ipAddress = (xForwardedFor != null && !xForwardedFor.isBlank()) 
                        ? xForwardedFor.split(",")[0].trim() 
                        : request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");
                String referrer = request.getHeader("Referer");
                analyticsService.logClick(shortUrl, ipAddress, userAgent, referrer);
            }

            // 3. Return 302 redirect
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(originalUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (IllegalArgumentException e) {
            // Short code not found → redirect to 404 page
            log.warn("Short code not found: {}", shortCode);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/?error=not-found"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (IllegalStateException e) {
            // Link expired → redirect with error
            log.warn("Expired link accessed: {}", shortCode);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/?error=expired"));
            return new ResponseEntity<>(headers, HttpStatus.GONE);
        }
    }
}
