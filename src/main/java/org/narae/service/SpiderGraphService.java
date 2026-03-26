package org.narae.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.narae.spidergraph.crawler.SpiderGraph;
import com.narae.spidergraph.model.PageGraph;
import com.narae.spidergraph.model.PageNode;
import org.narae.generated.model.CrawlRequest;
import org.narae.generated.model.CrawlResponse;
import org.narae.generated.model.PageNodeResponse;
import org.narae.config.ApiSecurityProperties;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

@Service
public class SpiderGraphService {
    private static final Logger logger = LogManager.getLogger(SpiderGraphService.class);
    private final Object crawlMonitor = new Object();
    private final ApiSecurityProperties properties;
    private final UrlSafetyValidator urlSafetyValidator;
    private final Semaphore crawlSemaphore;

    public SpiderGraphService(ApiSecurityProperties properties, UrlSafetyValidator urlSafetyValidator) {
        this.properties = properties;
        this.urlSafetyValidator = urlSafetyValidator;
        this.crawlSemaphore = new Semaphore(properties.getCrawl().getMaxConcurrentRequests(), true);
    }

    public CrawlResponse crawlSynchronously(CrawlRequest request) {
        return executeCrawl(request, "sync", true);
    }

    public CrawlResponse crawlAsynchronously(CrawlRequest request) {
        return executeCrawl(request, "async", false);
    }

    private CrawlResponse executeCrawl(CrawlRequest request, String mode, boolean synchronous) {
        String startUrl = normalizeAndValidateUrl(request.getStartUrl().toString(), "startUrl");
        String normalizedPrefix = normalizeOptionalUrl(uriToString(request.getUrlPrefix()), "urlPrefix");
        int maxDepth = orDefault(request.getMaxDepth(), 2);
        int timeout = orDefault(request.getTimeout(), 5000);
        int requestDelay = orDefault(request.getRequestDelay(), 0);
        String userAgent = orDefault(request.getUserAgent(), "SpiderGraphApi/1.0");
        boolean verifyHost = orDefault(request.getVerifyHost(), true);
        validateLimits(maxDepth, timeout, requestDelay);

        logger.info(
                "Starting {} crawl for {} with maxDepth={}, timeout={}, requestDelay={}, verifyHost={}, urlPrefix={}",
                mode,
                startUrl,
                maxDepth,
                timeout,
                requestDelay,
                verifyHost,
                normalizedPrefix
        );

        if (!crawlSemaphore.tryAcquire()) {
            throw new CrawlCapacityExceededException("Crawler capacity exceeded. Please retry later");
        }

        synchronized (crawlMonitor) {
            try {
                logger.debug("Acquired crawl monitor for {}", startUrl);
                resetCrawlerState();

                SpiderGraph.Crawler crawler = SpiderGraph.crawler()
                        .setMaxDepth(maxDepth)
                        .setTimeout(timeout)
                        .setRequestDelay(requestDelay)
                        .setUserAgent(userAgent)
                        .setVerifyHost(verifyHost)
                        .setCrawlStepHook(context -> {
                            boolean shouldContinue = context.graph().getNodes().size() < properties.getCrawl().getMaxPages();
                            if (!shouldContinue) {
                                logger.info(
                                        "Stopping {} crawl for {} after reaching configured maxPages={} at {}",
                                        mode,
                                        startUrl,
                                        properties.getCrawl().getMaxPages(),
                                        context.node().getUrl()
                                );
                            }
                            return shouldContinue;
                        });

                if (normalizedPrefix != null) {
                    crawler.setUrlPrefix(normalizedPrefix);
                }

                PageGraph graph = synchronous
                        ? crawler.startSynchronousSearch(startUrl)
                        : crawler.startAsynchronousSearch(startUrl);

                CrawlResponse response = new CrawlResponse();
                response.setMode("sync".equals(mode) ? CrawlResponse.ModeEnum.SYNC : CrawlResponse.ModeEnum.ASYNC);
                response.setStartUrl(URI.create(startUrl));
                response.setNodeCount(graph.getNodes().size());
                response.setNodes(mapNodes(graph.getNodes()));
                logger.info("Completed {} crawl for {} with {} nodes", mode, startUrl, response.getNodeCount());
                return response;
            } finally {
                crawlSemaphore.release();
            }
        }
    }

    private List<PageNodeResponse> mapNodes(Map<String, PageNode> nodes) {
        return nodes.values().stream()
                .sorted(Comparator.comparing(PageNode::getUrl))
                .map(this::toPageNodeResponse)
                .toList();
    }

    private PageNodeResponse toPageNodeResponse(PageNode node) {
        PageNodeResponse response = new PageNodeResponse();
        response.setUrl(URI.create(node.getUrl()));
        response.setTitle(node.getTitle());
        response.setText(node.getText());
        response.setOutgoingUrls(node.getOutgoing().stream().map(PageNode::getUrl).sorted().map(URI::create).toList());
        response.setIncomingUrls(node.getIncoming().stream().map(PageNode::getUrl).sorted().map(URI::create).toList());
        return response;
    }

    private String normalizeOptionalUrl(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeAndValidateUrl(value, fieldName);
    }

    private String normalizeAndValidateUrl(String value, String fieldName) {
        urlSafetyValidator.validatePublicHttpUrl(value, fieldName);

        URI uri = URI.create(value);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new InvalidCrawlRequestException(fieldName + " must be an absolute URL");
        }

        return uri.toString();
    }

    private String uriToString(URI value) {
        return value != null ? value.toString() : null;
    }

    private int orDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    private boolean orDefault(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    private String orDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private void validateLimits(int maxDepth, int timeout, int requestDelay) {
        if (maxDepth > properties.getCrawl().getMaxDepth()) {
            throw new InvalidCrawlRequestException("maxDepth exceeds the configured limit");
        }
        if (timeout > properties.getCrawl().getMaxTimeout()) {
            throw new InvalidCrawlRequestException("timeout exceeds the configured limit");
        }
        if (requestDelay > properties.getCrawl().getMaxRequestDelay()) {
            throw new InvalidCrawlRequestException("requestDelay exceeds the configured limit");
        }
        if (properties.getCrawl().getMaxPages() < 1) {
            throw new IllegalStateException("app.security.crawl.max-pages must be greater than zero");
        }
    }

    @SuppressWarnings("unchecked")
    private void resetCrawlerState() {
        try {
            logger.debug("Resetting spider-graph-lib crawler state");
            Class<?> searchClass = Class.forName("com.narae.spidergraph.crawler.Search");

            Field pageGraphField = searchClass.getDeclaredField("pageGraph");
            pageGraphField.setAccessible(true);
            PageGraph pageGraph = (PageGraph) pageGraphField.get(null);
            pageGraph.getNodes().clear();

            Field visitedField = searchClass.getDeclaredField("visited");
            visitedField.setAccessible(true);
            ((Set<String>) visitedField.get(null)).clear();

            Field lastRequestTimestampField = searchClass.getDeclaredField("lastRequestTimestamp");
            lastRequestTimestampField.setAccessible(true);
            lastRequestTimestampField.setLong(null, 0L);
            logger.debug("Crawler state reset completed");
        } catch (ReflectiveOperationException exception) {
            logger.error("Unable to reset spider-graph-lib crawler state", exception);
            throw new IllegalStateException("Unable to reset spider-graph-lib crawler state", exception);
        }
    }
}
