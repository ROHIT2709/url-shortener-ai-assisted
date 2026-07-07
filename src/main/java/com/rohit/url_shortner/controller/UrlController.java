package com.rohit.url_shortner.controller;

import com.rohit.url_shortner.dto.ShortenUrlRequest;
import com.rohit.url_shortner.dto.ShortenUrlResponse;
import com.rohit.url_shortner.dto.UrlStatsResponse;
import com.rohit.url_shortner.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Tag(name = "URL Management", description = "Create short URLs and read their statistics")
public class UrlController {

    private final UrlShortenerService service;

    @PostMapping
    @Operation(summary = "Shorten a URL",
            description = "Validates the URL (syntax + SSRF safety) and returns a base62 short code. "
                    + "Optional expiryDays (1-365) overrides the 30-day default.")
    @ApiResponse(responseCode = "201", description = "Short URL created")
    @ApiResponse(responseCode = "400", description = "Malformed, unsafe, or missing URL")
    public ResponseEntity<ShortenUrlResponse> shorten(@Valid @RequestBody ShortenUrlRequest request) {
        ShortenUrlResponse response = service.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}/stats")
    @Operation(summary = "Get statistics for a short URL",
            description = "Returns the original URL, creation/expiry timestamps, and total click count.")
    @ApiResponse(responseCode = "200", description = "Statistics found")
    @ApiResponse(responseCode = "404", description = "Unknown short code")
    public ResponseEntity<UrlStatsResponse> stats(@PathVariable String shortCode) {
        return ResponseEntity.ok(service.stats(shortCode));
    }
}
