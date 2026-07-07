package com.rohit.url_shortner.controller;

import com.rohit.url_shortner.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "Redirect", description = "Resolves short codes to their original URL")
public class RedirectController {

    private final UrlShortenerService service;

    // 302 (not 301) so browsers do not cache the redirect and every click reaches
    // the server for analytics. The regex keeps this mapping from swallowing
    // /api/** and /actuator/** paths, and the negative lookahead reserves the
    // single-segment paths used by Swagger (/docs) and the H2 console (/h2).
    @GetMapping("/{shortCode:(?!(?:docs|h2|api|actuator)$)[0-9a-zA-Z]{1,16}}")
    @Operation(summary = "Redirect to the original URL",
            description = "302 redirect so every click is counted (301 would let browsers cache "
                    + "the hop and bypass analytics).")
    @ApiResponse(responseCode = "302", description = "Redirect to the original URL")
    @ApiResponse(responseCode = "404", description = "Unknown short code")
    @ApiResponse(responseCode = "410", description = "Short URL has expired")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String target = service.resolve(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .build();
    }
}
