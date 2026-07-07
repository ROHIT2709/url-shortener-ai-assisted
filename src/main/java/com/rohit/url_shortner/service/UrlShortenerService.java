package com.rohit.url_shortner.service;

import com.rohit.url_shortner.dto.ShortenUrlRequest;
import com.rohit.url_shortner.dto.ShortenUrlResponse;
import com.rohit.url_shortner.dto.UrlStatsResponse;
import com.rohit.url_shortner.entity.UrlMapping;
import com.rohit.url_shortner.exception.UrlExpiredException;
import com.rohit.url_shortner.exception.UrlNotFoundException;
import com.rohit.url_shortner.repository.UrlMappingRepository;
import com.rohit.url_shortner.util.Base62;
import com.rohit.url_shortner.validation.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UrlShortenerService {

    private final UrlMappingRepository repository;
    private final UrlValidator urlValidator;
    private final String baseUrl;
    private final int defaultExpiryDays;

    public UrlShortenerService(UrlMappingRepository repository,
                               UrlValidator urlValidator,
                               @Value("${app.base-url}") String baseUrl,
                               @Value("${app.default-expiry-days}") int defaultExpiryDays) {
        this.repository = repository;
        this.urlValidator = urlValidator;
        this.baseUrl = baseUrl;
        this.defaultExpiryDays = defaultExpiryDays;
    }

    @Transactional
    public ShortenUrlResponse shorten(ShortenUrlRequest request) {
        urlValidator.validate(request.url());

        int expiryDays = request.expiryDays() != null ? request.expiryDays() : defaultExpiryDays;
        UrlMapping mapping = UrlMapping.builder()
                .originalUrl(request.url().trim())
                .expiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS))
                .build();

        // Save first to obtain the DB-generated id, then derive the short code from it.
        mapping = repository.save(mapping);
        mapping.setShortCode(Base62.encode(mapping.getId()));

        return toResponse(mapping);
    }

    @Transactional
    public String resolve(String shortCode) {
        UrlMapping mapping = findByShortCode(shortCode);
        if (mapping.isExpired()) {
            throw new UrlExpiredException(shortCode);
        }
        mapping.setClickCount(mapping.getClickCount() + 1);
        return mapping.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse stats(String shortCode) {
        UrlMapping mapping = findByShortCode(shortCode);
        return new UrlStatsResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getCreatedAt(),
                mapping.getExpiresAt(),
                mapping.getClickCount(),
                mapping.isExpired()
        );
    }

    private UrlMapping findByShortCode(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    private ShortenUrlResponse toResponse(UrlMapping mapping) {
        return new ShortenUrlResponse(
                mapping.getShortCode(),
                baseUrl + "/" + mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getCreatedAt(),
                mapping.getExpiresAt()
        );
    }
}
