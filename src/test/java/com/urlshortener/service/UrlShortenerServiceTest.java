package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.User;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private Base62Encoder base62Encoder;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlShortenerService, "baseUrl", "http://localhost:8080");
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();
    }

    @Test
    void testCreateShortUrlWithNewCustomAlias() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("my-alias")
                .build();

        when(urlRepository.existsByShortCode("my-alias")).thenReturn(false);
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        UrlResponse response = urlShortenerService.createShortUrl(request, testUser);

        assertNotNull(response);
        assertEquals("my-alias", response.getShortCode());
        assertEquals("http://localhost:8080/my-alias", response.getShortUrl());
        assertTrue(response.isCustomAlias());
        verify(urlRepository).existsByShortCode("my-alias");
        verify(urlRepository, times(2)).save(any(ShortUrl.class));
    }

    @Test
    void testCreateShortUrlWithExistingCustomAliasThrowsException() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("existing-alias")
                .build();

        when(urlRepository.existsByShortCode("existing-alias")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                urlShortenerService.createShortUrl(request, testUser));

        verify(urlRepository, never()).save(any(ShortUrl.class));
    }

    @Test
    void testCreateShortUrlAnonymous() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .build();

        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        when(base62Encoder.encode(42L)).thenReturn("000042");

        UrlResponse response = urlShortenerService.createShortUrl(request, null);

        assertNotNull(response);
        assertEquals("000042", response.getShortCode());
        assertEquals("http://localhost:8080/000042", response.getShortUrl());
        assertFalse(response.isCustomAlias());
        verify(urlRepository, times(2)).save(any(ShortUrl.class));
    }

    @Test
    void testGetOriginalUrlSuccess() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abcde")
                .originalUrl("https://example.com")
                .active(true)
                .build();

        when(urlRepository.findByShortCodeAndActiveTrue("abcde")).thenReturn(Optional.of(shortUrl));

        String original = urlShortenerService.getOriginalUrl("abcde");
        assertEquals("https://example.com", original);
    }

    @Test
    void testGetOriginalUrlNotFoundThrowsException() {
        when(urlRepository.findByShortCodeAndActiveTrue("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                urlShortenerService.getOriginalUrl("unknown"));
    }

    @Test
    void testGetOriginalUrlExpiredThrowsException() {
        ShortUrl expiredUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("expired")
                .originalUrl("https://example.com")
                .active(true)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(urlRepository.findByShortCodeAndActiveTrue("expired")).thenReturn(Optional.of(expiredUrl));

        assertThrows(IllegalStateException.class, () ->
                urlShortenerService.getOriginalUrl("expired"));
    }

    @Test
    void testGetUserUrls() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abcde")
                .originalUrl("https://example.com")
                .user(testUser)
                .active(true)
                .build();

        when(urlRepository.findByUserAndActiveTrueOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of(shortUrl));

        List<UrlResponse> urls = urlShortenerService.getUserUrls(testUser);
        assertNotNull(urls);
        assertEquals(1, urls.size());
        assertEquals("abcde", urls.get(0).getShortCode());
    }

    @Test
    void testDeleteUrlSuccess() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abcde")
                .originalUrl("https://example.com")
                .user(testUser)
                .active(true)
                .build();

        when(urlRepository.findByShortCodeAndActiveTrue("abcde")).thenReturn(Optional.of(shortUrl));

        urlShortenerService.deleteUrl("abcde", testUser);

        assertFalse(shortUrl.isActive());
        verify(urlRepository).save(shortUrl);
    }

    @Test
    void testDeleteUrlNotOwnerThrowsException() {
        User otherUser = User.builder().id(2L).name("Other").email("other@example.com").build();
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abcde")
                .originalUrl("https://example.com")
                .user(testUser)
                .active(true)
                .build();

        when(urlRepository.findByShortCodeAndActiveTrue("abcde")).thenReturn(Optional.of(shortUrl));

        assertThrows(SecurityException.class, () ->
                urlShortenerService.deleteUrl("abcde", otherUser));

        verify(urlRepository, never()).save(any(ShortUrl.class));
    }
}
