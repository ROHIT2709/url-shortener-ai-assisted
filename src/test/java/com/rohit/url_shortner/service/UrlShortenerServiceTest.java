package com.rohit.url_shortner.service;

import com.rohit.url_shortner.dto.ShortenUrlRequest;
import com.rohit.url_shortner.dto.ShortenUrlResponse;
import com.rohit.url_shortner.dto.UrlStatsResponse;
import com.rohit.url_shortner.entity.UrlMapping;
import com.rohit.url_shortner.exception.UrlExpiredException;
import com.rohit.url_shortner.exception.UrlNotFoundException;
import com.rohit.url_shortner.repository.UrlMappingRepository;
import com.rohit.url_shortner.validation.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String ORIGINAL_URL = "https://example.com/some/long/path";

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private UrlValidator urlValidator;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(repository, urlValidator, BASE_URL, 30);
    }

    @Test
    void shortenValidatesSavesAndDerivesCodeFromId() {
        when(repository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping m = invocation.getArgument(0);
            m.setId(125L); // base62(125) = "21"
            m.setCreatedAt(Instant.now());
            return m;
        });

        ShortenUrlResponse response = service.shorten(new ShortenUrlRequest(ORIGINAL_URL, null));

        verify(urlValidator).validate(ORIGINAL_URL);
        assertEquals("21", response.shortCode());
        assertEquals(BASE_URL + "/21", response.shortUrl());
        assertEquals(ORIGINAL_URL, response.originalUrl());
    }

    @Test
    void shortenAppliesDefaultExpiryWhenNotProvided() {
        when(repository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping m = invocation.getArgument(0);
            m.setId(1L);
            m.setCreatedAt(Instant.now());
            return m;
        });

        ShortenUrlResponse response = service.shorten(new ShortenUrlRequest(ORIGINAL_URL, null));

        Instant expectedAround = Instant.now().plus(30, ChronoUnit.DAYS);
        assertTrue(Math.abs(response.expiresAt().getEpochSecond() - expectedAround.getEpochSecond()) < 60);
    }

    @Test
    void shortenHonoursRequestedExpiry() {
        when(repository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping m = invocation.getArgument(0);
            m.setId(1L);
            m.setCreatedAt(Instant.now());
            return m;
        });

        ShortenUrlResponse response = service.shorten(new ShortenUrlRequest(ORIGINAL_URL, 7));

        Instant expectedAround = Instant.now().plus(7, ChronoUnit.DAYS);
        assertTrue(Math.abs(response.expiresAt().getEpochSecond() - expectedAround.getEpochSecond()) < 60);
    }

    @Test
    void resolveReturnsOriginalUrlAndIncrementsClicks() {
        UrlMapping mapping = activeMapping();
        when(repository.findByShortCode("abc")).thenReturn(Optional.of(mapping));

        String target = service.resolve("abc");

        assertEquals(ORIGINAL_URL, target);
        assertEquals(1, mapping.getClickCount());
    }

    @Test
    void resolveThrowsNotFoundForUnknownCode() {
        when(repository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> service.resolve("nope"));
    }

    @Test
    void resolveThrowsGoneForExpiredMapping() {
        UrlMapping mapping = activeMapping();
        mapping.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(repository.findByShortCode("abc")).thenReturn(Optional.of(mapping));

        assertThrows(UrlExpiredException.class, () -> service.resolve("abc"));
        assertEquals(0, mapping.getClickCount());
    }

    @Test
    void statsReturnsMappingDetailsWithoutIncrementingClicks() {
        UrlMapping mapping = activeMapping();
        mapping.setClickCount(5);
        when(repository.findByShortCode("abc")).thenReturn(Optional.of(mapping));

        UrlStatsResponse stats = service.stats("abc");

        assertEquals("abc", stats.shortCode());
        assertEquals(ORIGINAL_URL, stats.originalUrl());
        assertEquals(5, stats.clickCount());
        assertEquals(5, mapping.getClickCount());
    }

    @Test
    void statsThrowsNotFoundForUnknownCode() {
        when(repository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> service.stats("nope"));
    }

    private UrlMapping activeMapping() {
        return UrlMapping.builder()
                .id(1L)
                .shortCode("abc")
                .originalUrl(ORIGINAL_URL)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
    }
}
