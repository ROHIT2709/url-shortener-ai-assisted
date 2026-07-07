package com.rohit.url_shortner.validation;

import com.rohit.url_shortner.exception.InvalidUrlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlValidatorTest {

    private final UrlValidator validator = new UrlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com",
            "https://example.com/path?query=1&other=2",
            "https://sub.domain.example.com:8443/deep/path#fragment"
    })
    void acceptsValidUrls(String url) {
        assertDoesNotThrow(() -> validator.validate(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankUrls(String url) {
        assertThrows(InvalidUrlException.class, () -> validator.validate(url));
    }

    @Test
    void rejectsNullUrl() {
        assertThrows(InvalidUrlException.class, () -> validator.validate(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file",
            "javascript:alert(1)",
            "file:///etc/passwd",
            "example.com",
            "//example.com"
    })
    void rejectsDisallowedSchemes(String url) {
        assertThrows(InvalidUrlException.class, () -> validator.validate(url));
    }

    @Test
    void rejectsMalformedUrl() {
        assertThrows(InvalidUrlException.class, () -> validator.validate("http://exa mple.com/^bad"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost/admin",
            "http://app.localhost/admin",
            "http://127.0.0.1/admin",
            "http://10.0.0.5/internal",
            "http://192.168.1.1/router",
            "http://172.16.0.1/internal",
            "http://[::1]/admin",
            "http://0.0.0.0/"
    })
    void rejectsLoopbackAndPrivateTargets(String url) {
        assertThrows(InvalidUrlException.class, () -> validator.validate(url));
    }

    @Test
    void rejectsOverlongUrl() {
        String url = "https://example.com/" + "a".repeat(2100);
        assertThrows(InvalidUrlException.class, () -> validator.validate(url));
    }
}
