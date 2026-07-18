package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Analytics summary for a short URL */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private Long urlId;
    private String shortCode;
    private String originalUrl;
    private long totalClicks;

    /** e.g. {"India": 150, "USA": 80, "Unknown": 20} */
    private Map<String, Long> clicksByCountry;

    /** e.g. {"DESKTOP": 120, "MOBILE": 100, "TABLET": 30} */
    private Map<String, Long> clicksByDevice;

    /** e.g. {"Chrome": 140, "Firefox": 60, "Safari": 50} */
    private Map<String, Long> clicksByBrowser;
}
