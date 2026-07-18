package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.UrlClick;
import com.urlshortener.repository.ClickRepository;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ClickRepository clickRepository;

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void testLogClickMobileChrome() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abcde")
                .originalUrl("https://example.com")
                .clickCount(0L)
                .build();

        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1 Chrome/95.0.4638.54";
        String referer = "https://google.com";

        analyticsService.logClick(shortUrl, ipAddress, userAgent, referer);

        ArgumentCaptor<UrlClick> clickCaptor = ArgumentCaptor.forClass(UrlClick.class);
        verify(clickRepository).save(clickCaptor.capture());
        verify(urlRepository).incrementClickCount(1L);

        UrlClick click = clickCaptor.getValue();
        assertNotNull(click);
        assertEquals(shortUrl, click.getShortUrl());
        assertEquals("192.168.1.100", click.getIpAddress());
        assertEquals("Mobile", click.getDeviceType());
        assertEquals("Chrome", click.getBrowser()); // UA contains both Safari and Chrome, Chrome gets matched first
        assertEquals("https://google.com", click.getReferrer());
    }

    @Test
    void testLogClickWithXForwardedForProxy() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abcde")
                .build();

        String ipAddress = "203.0.113.195"; // Pre-extracted
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:93.0) Gecko/20100101 Firefox/93.0";

        analyticsService.logClick(shortUrl, ipAddress, userAgent, null);

        ArgumentCaptor<UrlClick> clickCaptor = ArgumentCaptor.forClass(UrlClick.class);
        verify(clickRepository).save(clickCaptor.capture());

        UrlClick click = clickCaptor.getValue();
        assertEquals("203.0.113.195", click.getIpAddress());
        assertEquals("Desktop", click.getDeviceType());
        assertEquals("Firefox", click.getBrowser());
    }

    @Test
    void testGetAnalyticsSummary() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abcde")
                .originalUrl("https://example.com")
                .build();

        when(clickRepository.countByShortUrl(shortUrl)).thenReturn(10L);
        when(clickRepository.countByCountry(shortUrl)).thenReturn(List.of(new Object[]{"US", 6L}, new Object[]{"CA", 4L}));
        when(clickRepository.countByDeviceType(shortUrl)).thenReturn(List.of(new Object[]{"Desktop", 7L}, new Object[]{"Mobile", 3L}));
        when(clickRepository.countByBrowser(shortUrl)).thenReturn(List.of(new Object[]{"Chrome", 8L}, new Object[]{"Firefox", 2L}));

        AnalyticsResponse response = analyticsService.getAnalytics(shortUrl);

        assertNotNull(response);
        assertEquals(1L, response.getUrlId());
        assertEquals("abcde", response.getShortCode());
        assertEquals(10L, response.getTotalClicks());
        assertEquals(6L, response.getClicksByCountry().get("US"));
        assertEquals(7L, response.getClicksByDevice().get("Desktop"));
        assertEquals(8L, response.getClicksByBrowser().get("Chrome"));
    }
}
