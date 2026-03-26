package org.narae.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.narae.config.ApiSecurityProperties;
import org.narae.generated.model.CrawlRequest;
import org.narae.generated.model.CrawlResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpiderGraphServiceTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldStopCrawlAtConfiguredMaxPagesAndReturnPartialGraph() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> writeHtml(exchange.getResponseBody(), exchange, """
                <html><body>
                <a href="/page-1">Page 1</a>
                <a href="/page-2">Page 2</a>
                </body></html>
                """));
        server.createContext("/page-1", exchange -> writeHtml(exchange.getResponseBody(), exchange, """
                <html><body><h1>Page 1</h1></body></html>
                """));
        server.createContext("/page-2", exchange -> writeHtml(exchange.getResponseBody(), exchange, """
                <html><body><h1>Page 2</h1></body></html>
                """));
        server.start();

        ApiSecurityProperties properties = new ApiSecurityProperties();
        properties.getCrawl().setMaxPages(2);
        properties.getCrawl().setMaxDepth(3);
        properties.getCrawl().setMaxTimeout(5_000);
        properties.getCrawl().setMaxRequestDelay(0);

        UrlSafetyValidator validator = new UrlSafetyValidator(properties) {
            @Override
            public void validatePublicHttpUrl(String value, String fieldName) {
                // Allow the embedded test server bound to localhost.
            }
        };

        SpiderGraphService service = new SpiderGraphService(properties, validator);

        CrawlRequest request = new CrawlRequest();
        request.setStartUrl(URI.create("http://localhost:" + server.getAddress().getPort() + "/"));
        request.setMaxDepth(3);
        request.setTimeout(5_000);
        request.setRequestDelay(0);
        request.setVerifyHost(false);

        CrawlResponse response = service.crawlSynchronously(request);

        assertNotNull(response);
        assertEquals(2, response.getNodeCount());
        assertEquals(
                Set.of(
                        request.getStartUrl().toString(),
                        "http://localhost:" + server.getAddress().getPort() + "/page-1"
                ),
                response.getNodes().stream().map(node -> node.getUrl().toString()).collect(java.util.stream.Collectors.toSet())
        );
    }

    private static void writeHtml(OutputStream responseBody, com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (exchange; responseBody) {
            responseBody.write(bytes);
        }
    }
}
