package com.project.ip;

public class IpLookupException extends Exception {
    public IpLookupException(String message) {
        super(message);
    }

    public IpLookupException(String message, Throwable cause) {
        super(message, cause);
    }
} 