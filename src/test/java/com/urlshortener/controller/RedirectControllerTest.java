package com.urlshortener.controller;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlShortenerService urlShortenerService;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private UserDetailsService userDetailsService; // keeps spring security happy

    @Test
    void testRedirectSuccess() throws Exception {
        String shortCode = "abcde";
        String originalUrl = "https://example.com";
        ShortUrl shortUrl = ShortUrl.builder().id(1L).shortCode(shortCode).originalUrl(originalUrl).build();

        when(urlShortenerService.getOriginalUrl(shortCode)).thenReturn(originalUrl);
        when(urlShortenerService.findByShortCode(shortCode)).thenReturn(shortUrl);

        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));

        verify(analyticsService).logClick(eq(shortUrl), any(), any(), any());
    }

    @Test
    void testRedirectNotFound() throws Exception {
        String shortCode = "notfound";
        when(urlShortenerService.getOriginalUrl(shortCode)).thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/?error=not-found"));
    }

    @Test
    void testRedirectExpired() throws Exception {
        String shortCode = "expired";
        when(urlShortenerService.getOriginalUrl(shortCode)).thenThrow(new IllegalStateException("Expired"));

        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isGone())
                .andExpect(header().string("Location", "/?error=expired"));
    }
}
