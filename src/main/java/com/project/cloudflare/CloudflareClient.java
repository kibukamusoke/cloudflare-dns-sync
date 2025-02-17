package com.project.cloudflare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CloudflareClient {
    private static final Logger logger = LoggerFactory.getLogger(CloudflareClient.class);
    private static final String API_BASE = "https://api.cloudflare.com/client/v4";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiToken;
    private final String zoneId;

    public CloudflareClient(String apiToken, String zoneId) {
        this.apiToken = apiToken;
        this.zoneId = zoneId;
    }

    public DnsRecord getDnsRecord(String recordName, String recordType) throws CloudflareException {
        String url = String.format("%s/zones/%s/dns_records?name=%s&type=%s", 
            API_BASE, zoneId, recordName, recordType);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", "Bearer " + apiToken);
            
            return client.execute(request, response -> {
                try {
                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    if (response.getCode() != 200) {
                        throw new RuntimeException(new CloudflareException("Failed to get DNS record. Status: " + response.getCode() 
                            + ", Response: " + jsonResponse));
                    }

                    JsonNode root = objectMapper.readTree(jsonResponse);
                    if (!root.path("success").asBoolean()) {
                        throw new RuntimeException(new CloudflareException("API request failed: " + jsonResponse));
                    }

                    JsonNode records = root.path("result");
                    if (records.isEmpty()) {
                        return null;
                    }

                    JsonNode record = records.get(0);
                    return new DnsRecord(
                        record.path("id").asText(),
                        record.path("name").asText(),
                        record.path("type").asText(),
                        record.path("content").asText()
                    );
                } catch (IOException e) {
                    throw new RuntimeException("Error processing response", e);
                }
            });
        } catch (Exception e) {
            throw new CloudflareException("Error getting DNS record", e);
        }
    }

    public void updateDnsRecord(String id, 
                              String name, 
                              String type, 
                              String content) throws CloudflareException {
        try {
            String url = String.format("%s/zones/%s/dns_records/%s", API_BASE, zoneId, id);
            
            Map<String, String> data = new HashMap<>();
            data.put("type", type);
            data.put("name", name);
            data.put("content", content);
            
            String json = objectMapper.writeValueAsString(data);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPut request = new HttpPut(url);
                request.setHeader("Authorization", "Bearer " + apiToken);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

                client.execute(request, response -> {
                    try {
                        String jsonResponse = EntityUtils.toString(response.getEntity());
                        if (response.getCode() != 200) {
                            throw new RuntimeException(new CloudflareException("Failed to update DNS record. Status: " + response.getCode() 
                                + ", Response: " + jsonResponse));
                        }

                        JsonNode root = objectMapper.readTree(jsonResponse);
                        if (!root.path("success").asBoolean()) {
                            throw new RuntimeException(new CloudflareException("API request failed: " + jsonResponse));
                        }
                        
                        logger.info("Successfully updated DNS record {} to {}", name, content);
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException("Error processing response", e);
                    }
                });
            }
        } catch (Exception e) {
            throw new CloudflareException("Error updating DNS record", e);
        }
    }
} 