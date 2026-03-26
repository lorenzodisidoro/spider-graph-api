package org.narae.service;

import org.junit.jupiter.api.Test;
import org.narae.config.ApiSecurityProperties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlSafetyValidatorTest {
    private final UrlSafetyValidator validator = new UrlSafetyValidator(new ApiSecurityProperties());

    @Test
    void shouldRejectLoopbackHosts() {
        assertThrows(
                InvalidCrawlRequestException.class,
                () -> validator.validatePublicHttpUrl("http://127.0.0.1:8080/test", "startUrl")
        );
    }

    @Test
    void shouldRejectUnsupportedSchemes() {
        assertThrows(
                InvalidCrawlRequestException.class,
                () -> validator.validatePublicHttpUrl("file:///etc/passwd", "startUrl")
        );
    }

    @Test
    void shouldAllowPublicHttpsHosts() {
        assertDoesNotThrow(() -> validator.validatePublicHttpUrl("https://8.8.8.8", "startUrl"));
    }

    @Test
    void shouldRejectNonStandardPorts() {
        assertThrows(
                InvalidCrawlRequestException.class,
                () -> validator.validatePublicHttpUrl("https://example.com:8443", "startUrl")
        );
    }
}
