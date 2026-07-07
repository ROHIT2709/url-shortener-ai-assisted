package com.rohit.url_shortner.validation;

import com.rohit.url_shortner.exception.InvalidUrlException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Two-layer URL validation:
 * Layer 1 (syntactic): well-formed URI, allowed scheme, host present, length bound.
 * Layer 2 (safety): rejects loopback/private/link-local targets to prevent SSRF-style
 * redirects into internal infrastructure.
 */
@Component
public class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int MAX_URL_LENGTH = 2048;

    public void validate(String url) {
        URI uri = validateSyntax(url);
        validateSafety(uri);
    }

    private URI validateSyntax(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL must not be blank");
        }
        if (url.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL exceeds maximum length of " + MAX_URL_LENGTH);
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("URL is not well-formed");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new InvalidUrlException("URL scheme must be http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("URL must contain a valid host");
        }
        return uri;
    }

    private void validateSafety(URI uri) {
        String host = uri.getHost().toLowerCase();
        if (host.equals("localhost") || host.endsWith(".localhost")) {
            throw new InvalidUrlException("URLs targeting localhost are not allowed");
        }
        InetAddress address = parseIpLiteral(host);
        if (address != null && isDisallowedAddress(address)) {
            throw new InvalidUrlException("URLs targeting private or loopback addresses are not allowed");
        }
    }

    /**
     * Only inspects IP literals; hostnames are not DNS-resolved here to keep
     * validation fast and side-effect free.
     */
    private InetAddress parseIpLiteral(String host) {
        String candidate = host;
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (!candidate.matches("^[0-9.]+$") && !candidate.contains(":")) {
            return null;
        }
        try {
            return InetAddress.getByName(candidate);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private boolean isDisallowedAddress(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }
}
