package com.urlshortener.controller;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlShortenerService urlShortenerService;

    @MockBean
    private UserDetailsService userDetailsService; // keeps spring security happy

    @Test
    void testGetQrCodeSuccess() throws Exception {
        String shortCode = "abcde";
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode(shortCode)
                .originalUrl("https://example.com")
                .build();

        when(urlShortenerService.findByShortCode(shortCode)).thenReturn(shortUrl);

        mockMvc.perform(get("/api/urls/{shortCode}/qr", shortCode))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().exists("Cache-Control"));
    }

    @Test
    void testGetQrCodeNotFound() throws Exception {
        String shortCode = "notfound";
        when(urlShortenerService.findByShortCode(shortCode)).thenReturn(null);

        mockMvc.perform(get("/api/urls/{shortCode}/qr", shortCode))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPublicQrCodeSuccess() throws Exception {
        String customUrl = "https://linkedin.com/in/siddhesh-ramteke";

        mockMvc.perform(get("/api/public/qr").param("url", customUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().exists("Cache-Control"));
    }

    @Test
    void testGetPublicQrCodeBadRequest() throws Exception {
        mockMvc.perform(get("/api/public/qr").param("url", ""))
                .andExpect(status().isBadRequest());
    }
}
