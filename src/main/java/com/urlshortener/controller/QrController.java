package com.urlshortener.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;

/**
 * QR Code Controller.
 * GET /api/urls/{shortCode}/qr → returns the QR code representing the short URL redirect as a PNG image.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class QrController {

    private final UrlShortenerService urlShortenerService;

    @GetMapping(value = "/api/urls/{shortCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable String shortCode, HttpServletRequest request) {
        try {
            // 1. Verify URL exists
            ShortUrl shortUrl = urlShortenerService.findByShortCode(shortCode);
            if (shortUrl == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 2. Build full Short URL dynamically from request context
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String shortLinkUrl = scheme + "://" + serverName + (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort) + "/" + shortCode;

            // 3. Generate QR code matrix using ZXing
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(shortLinkUrl, BarcodeFormat.QR_CODE, 250, 250);

            // 4. Write image to byte array
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] imageBytes = pngOutputStream.toByteArray();

            // 5. Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("max-age=31536000"); // Cache it as it doesn't change

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed to generate QR Code for short code {}: {}", shortCode, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/api/public/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPublicQrCode(@RequestParam String url) {
        try {
            if (url == null || url.isBlank()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Generate QR code matrix for the custom URL
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] imageBytes = pngOutputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("max-age=31536000"); // Cache it as it doesn't change

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed to generate custom QR Code for url {}: {}", url, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
