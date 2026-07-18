package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/** Request body for creating a new short URL */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {

    @NotBlank(message = "URL is required")
    @URL(message = "Please enter a valid URL (must start with http:// or https://)")
    private String originalUrl;

    /** Optional user-defined alias (e.g. "my-promo") */
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,20}$",
             message = "Alias must be 3-20 characters: letters, numbers, - or _")
    private String customAlias;

    /** Optional description / title */
    private String title;

    /**
     * Optional expiry in hours from now.
     * Null = never expires.
     */
    private Integer expiryHours;
}
