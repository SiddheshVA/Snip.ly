package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.UrlClick;
import com.urlshortener.repository.ClickRepository;
import com.urlshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics service — logs click data and provides analytics summaries.
 *
 * Click logging is @Async → non-blocking.
 * The redirect happens INSTANTLY, and click recording happens in the background.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickRepository clickRepository;
    private final UrlRepository urlRepository;

    /**
     * Log a click asynchronously — doesn't slow down the redirect at all.
     * Runs in a separate thread pool (configured by @EnableAsync).
     */
    @Async
    @Transactional
    public void logClick(ShortUrl shortUrl, String ipAddress, String userAgent, String referrer) {
        try {
            UrlClick click = UrlClick.builder()
                    .shortUrl(shortUrl)
                    .ipAddress(ipAddress)
                    .deviceType(detectDevice(userAgent))
                    .browser(detectBrowser(userAgent))
                    .referrer(referrer != null ? referrer : "Direct")
                    .country("Unknown")   // Upgrade: integrate ip-api.com for geolocation
                    .city("Unknown")
                    .build();

            clickRepository.save(click);
            urlRepository.incrementClickCount(shortUrl.getId());

            log.debug("Logged click for {}: IP={}, Device={}", shortUrl.getShortCode(), ipAddress, click.getDeviceType());
        } catch (Exception e) {
            log.error("Failed to log click for {}: {}", shortUrl.getShortCode(), e.getMessage());
        }
    }

    /** Build analytics summary for a short URL */
    public AnalyticsResponse getAnalytics(ShortUrl shortUrl) {
        long totalClicks = clickRepository.countByShortUrl(shortUrl);

        Map<String, Long> byCountry  = toMap(clickRepository.countByCountry(shortUrl));
        Map<String, Long> byDevice   = toMap(clickRepository.countByDeviceType(shortUrl));
        Map<String, Long> byBrowser  = toMap(clickRepository.countByBrowser(shortUrl));

        return AnalyticsResponse.builder()
                .urlId(shortUrl.getId())
                .shortCode(shortUrl.getShortCode())
                .originalUrl(shortUrl.getOriginalUrl())
                .totalClicks(totalClicks)
                .clicksByCountry(byCountry)
                .clicksByDevice(byDevice)
                .clicksByBrowser(byBrowser)
                .build();
    }

    // ===== Private Helpers =====

    /** Get real client IP (handles proxies / load balancers) */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Simple User-Agent → device type detection */
    private String detectDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "Mobile";
        if (ua.contains("tablet") || ua.contains("ipad")) return "Tablet";
        return "Desktop";
    }

    /** Simple User-Agent → browser detection */
    private String detectBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/"))    return "Edge";
        if (ua.contains("chrome"))  return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari"))  return "Safari";
        if (ua.contains("opera"))   return "Opera";
        return "Other";
    }

    /** Convert JPQL result (Object[]{label, count}) to Map */
    private Map<String, Long> toMap(List<Object[]> results) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : results) {
            String label = row[0] != null ? row[0].toString() : "Unknown";
            Long count = ((Number) row[1]).longValue();
            map.put(label, count);
        }
        return map;
    }
}
