package org.narae.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public class ApiSecurityProperties {
    private final Cors cors = new Cors();
    private final RateLimit rateLimit = new RateLimit();
    private final Crawl crawl = new Crawl();
    private final ForwardHeaders forwardHeaders = new ForwardHeaders();

    public Cors getCors() {
        return cors;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Crawl getCrawl() {
        return crawl;
    }

    public ForwardHeaders getForwardHeaders() {
        return forwardHeaders;
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class RateLimit {
        private int maxRequestsPerMinute = 10;

        public int getMaxRequestsPerMinute() {
            return maxRequestsPerMinute;
        }

        public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
        }
    }

    public static class Crawl {
        private int maxConcurrentRequests = 1;
        private int maxDepth = 3;
        private int maxTimeout = 10000;
        private int maxRequestDelay = 2000;
        private int maxPages = 50;
        private List<Integer> allowedPorts = new ArrayList<>(List.of(80, 443));

        public int getMaxConcurrentRequests() {
            return maxConcurrentRequests;
        }

        public void setMaxConcurrentRequests(int maxConcurrentRequests) {
            this.maxConcurrentRequests = maxConcurrentRequests;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        public int getMaxTimeout() {
            return maxTimeout;
        }

        public void setMaxTimeout(int maxTimeout) {
            this.maxTimeout = maxTimeout;
        }

        public int getMaxRequestDelay() {
            return maxRequestDelay;
        }

        public void setMaxRequestDelay(int maxRequestDelay) {
            this.maxRequestDelay = maxRequestDelay;
        }

        public int getMaxPages() {
            return maxPages;
        }

        public void setMaxPages(int maxPages) {
            this.maxPages = maxPages;
        }

        public List<Integer> getAllowedPorts() {
            return allowedPorts;
        }

        public void setAllowedPorts(List<Integer> allowedPorts) {
            this.allowedPorts = allowedPorts;
        }
    }

    public static class ForwardHeaders {
        private boolean trustXForwardedFor = false;

        public boolean isTrustXForwardedFor() {
            return trustXForwardedFor;
        }

        public void setTrustXForwardedFor(boolean trustXForwardedFor) {
            this.trustXForwardedFor = trustXForwardedFor;
        }
    }
}
