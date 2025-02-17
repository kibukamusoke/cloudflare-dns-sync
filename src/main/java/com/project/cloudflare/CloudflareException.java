package com.project.cloudflare;

public class CloudflareException extends Exception {
    public CloudflareException(String message) {
        super(message);
    }

    public CloudflareException(String message, Throwable cause) {
        super(message, cause);
    }
} 