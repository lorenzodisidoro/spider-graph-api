package org.narae.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.narae.generated.model.CrawlResponse;
import org.narae.service.SpiderGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CrawlControllerIntegrationTest {
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
    void shouldExposeSyncCrawlEndpoint() throws Exception {
        mockMvc.perform(post("/api/crawls/sync")
                        .contentType("application/json")
                        .content("""
                                {
                                  "startUrl": "https://www.geeksforgeeks.org/advance-java/advanced-java/",
                                  "maxDepth": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("sync"))
                .andExpect(jsonPath("$.nodeCount").value(1))
                .andExpect(jsonPath("$.startUrl").value("https://www.geeksforgeeks.org/advance-java/advanced-java/"));
    }

    @Test
    void shouldAllowCorsForDevFrontend() throws Exception {
        mockMvc.perform(options("/api/crawls/sync")
                        .header(ORIGIN, "http://localhost:3000")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
