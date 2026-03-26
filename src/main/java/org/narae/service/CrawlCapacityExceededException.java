package org.narae.service;

public class CrawlCapacityExceededException extends RuntimeException {
    public CrawlCapacityExceededException(String message) {
        super(message);
    }
}
