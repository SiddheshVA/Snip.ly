package com.urlshortener.repository;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.UrlClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ClickRepository extends JpaRepository<UrlClick, Long> {

    /** All clicks for a given short URL */
    List<UrlClick> findByShortUrlOrderByClickedAtDesc(ShortUrl shortUrl);

    /** Total click count for a short URL */
    long countByShortUrl(ShortUrl shortUrl);

    /** Clicks grouped by country (for pie chart analytics) */
    @Query("SELECT c.country, COUNT(c) FROM UrlClick c WHERE c.shortUrl = :shortUrl GROUP BY c.country")
    List<Object[]> countByCountry(ShortUrl shortUrl);

    /** Clicks grouped by device type */
    @Query("SELECT c.deviceType, COUNT(c) FROM UrlClick c WHERE c.shortUrl = :shortUrl GROUP BY c.deviceType")
    List<Object[]> countByDeviceType(ShortUrl shortUrl);

    /** Clicks grouped by browser */
    @Query("SELECT c.browser, COUNT(c) FROM UrlClick c WHERE c.shortUrl = :shortUrl GROUP BY c.browser")
    List<Object[]> countByBrowser(ShortUrl shortUrl);
}
