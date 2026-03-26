package org.narae.service;

public class InvalidCrawlRequestException extends RuntimeException {
    public InvalidCrawlRequestException(String message) {
        super(message);
    }
}
