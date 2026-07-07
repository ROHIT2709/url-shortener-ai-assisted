package com.rohit.url_shortner.controller;

import com.rohit.url_shortner.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService service;

    // 302 (not 301) so browsers do not cache the redirect and every click reaches
    // the server for analytics. The regex keeps this mapping from swallowing
    // /api/** and /actuator/** paths.
    @GetMapping("/{shortCode:[0-9a-zA-Z]{1,16}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String target = service.resolve(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .build();
    }
}
