package org.narae.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.narae.generated.model.CrawlResponse;
import org.narae.service.SpiderGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.rate-limit.max-requests-per-minute=2",
        "app.security.forward-headers.trust-x-forwarded-for=true"
})
class RateLimitFilterIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpiderGraphService spiderGraphService;

    @BeforeEach
    void setUp() {
        CrawlResponse response = new CrawlResponse();
        response.setMode(CrawlResponse.ModeEnum.SYNC);
        response.setStartUrl(URI.create("https://www.geeksforgeeks.org/advance-java/advanced-java/"));
        response.setNodeCount(1);

        when(spiderGraphService.crawlSynchronously(any())).thenReturn(response);
    }

    @Test
    void shouldReturnTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
        String payload = """
                {
                  "startUrl": "https://www.geeksforgeeks.org/advance-java/advanced-java/",
                  "maxDepth": 1
                }
                """;

        mockMvc.perform(post("/api/crawls/sync")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/crawls/sync")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/crawls/sync")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Rate limit exceeded"));
    }
}
