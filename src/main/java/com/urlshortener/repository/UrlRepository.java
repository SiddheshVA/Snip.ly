package com.urlshortener.repository;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<ShortUrl, Long> {

    /** Find by short code — used on every redirect */
    Optional<ShortUrl> findByShortCodeAndActiveTrue(String shortCode);

    /** Check if a custom alias already exists */
    boolean existsByShortCode(String shortCode);

    /** Get all active URLs for a user (dashboard) */
    List<ShortUrl> findByUserAndActiveTrueOrderByCreatedAtDesc(User user);

    /** Increment click count atomically */
    @Modifying
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1 WHERE s.id = :id")
    void incrementClickCount(Long id);
}
