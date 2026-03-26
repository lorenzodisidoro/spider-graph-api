package org.narae.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.narae.generated.api.ApiApi;
import org.narae.generated.model.CrawlRequest;
import org.narae.generated.model.CrawlResponse;
import org.narae.service.SpiderGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CrawlController implements ApiApi {
    private static final Logger logger = LogManager.getLogger(CrawlController.class);
    private final SpiderGraphService spiderGraphService;

    public CrawlController(SpiderGraphService spiderGraphService) {
        this.spiderGraphService = spiderGraphService;
    }

    @Override
    public ResponseEntity<CrawlResponse> crawlSync(CrawlRequest crawlRequest) {
        logger.info("Received synchronous crawl request for {}", crawlRequest.getStartUrl());
        CrawlResponse response = spiderGraphService.crawlSynchronously(crawlRequest);
        logger.info("Synchronous crawl completed for {} with {} nodes", response.getStartUrl(), response.getNodeCount());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CrawlResponse> crawlAsync(CrawlRequest crawlRequest) {
        logger.info("Received asynchronous crawl request for {}", crawlRequest.getStartUrl());
        CrawlResponse response = spiderGraphService.crawlAsynchronously(crawlRequest);
        logger.info("Asynchronous crawl completed for {} with {} nodes", response.getStartUrl(), response.getNodeCount());
        return ResponseEntity.ok(response);
    }
}
