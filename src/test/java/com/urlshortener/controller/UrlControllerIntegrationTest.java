package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.User;
import com.urlshortener.security.JwtTokenProvider;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UrlShortenerService urlShortenerService;

    @MockBean
    private AnalyticsService analyticsService;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .role(User.Role.USER)
                .build();

        // Generate token and configure UserDetailsService mock
        jwtToken = jwtTokenProvider.generateToken(testUser);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUser);
    }

    @Test
    void testAnonymousPostAllowed() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .build();

        UrlResponse expectedResponse = UrlResponse.builder()
                .id(1L)
                .shortCode("00000y")
                .shortUrl("http://localhost:8080/00000y")
                .originalUrl("https://example.com")
                .clickCount(0)
                .active(true)
                .build();

        // In SecurityConfig we permitted HttpMethod.POST, "/api/urls"
        when(urlShortenerService.createShortUrl(any(CreateUrlRequest.class), any())).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("00000y"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"));
    }

    @Test
    void testAnonymousGetListForbidden() throws Exception {
        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isForbidden()); // or isUnauthorized() depending on entrypoint config, spring security defaults to 403 Forbidden
    }

    @Test
    void testAuthenticatedGetListSuccess() throws Exception {
        UrlResponse expectedResponse = UrlResponse.builder()
                .id(1L)
                .shortCode("00000y")
                .shortUrl("http://localhost:8080/00000y")
                .originalUrl("https://example.com")
                .clickCount(0)
                .active(true)
                .build();

        when(urlShortenerService.getUserUrls(any(User.class))).thenReturn(Collections.singletonList(expectedResponse));

        mockMvc.perform(get("/api/urls")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortCode").value("00000y"));
    }

    @Test
    void testAnonymousDeleteForbidden() throws Exception {
        mockMvc.perform(delete("/api/urls/00000y"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAuthenticatedDeleteSuccess() throws Exception {
        mockMvc.perform(delete("/api/urls/00000y")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }
}
