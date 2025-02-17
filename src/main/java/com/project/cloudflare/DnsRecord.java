package com.project.cloudflare;

public class DnsRecord {
    private final String id;
    private final String name;
    private final String type;
    private final String content;

    public DnsRecord(String id, String name, String type, String content) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
} 