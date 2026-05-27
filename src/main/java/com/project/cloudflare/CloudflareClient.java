package com.project.cloudflare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Thin Cloudflare DNS API client. A single instance (and its underlying HTTP
 * client) is shared across all managed records; the API token and zone are
 * passed per call so one client can serve multiple zones, domains and tokens.
 */
public class CloudflareClient {
    private static final Logger logger = LoggerFactory.getLogger(CloudflareClient.class);
    private static final String API_BASE = "https://api.cloudflare.com/client/v4";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CloseableHttpClient httpClient;

    public CloudflareClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public DnsRecord getDnsRecord(String apiToken, String zoneId, String recordName, String recordType)
            throws CloudflareException {
        String url = String.format("%s/zones/%s/dns_records?name=%s&type=%s",
            API_BASE, encode(zoneId), encode(recordName), encode(recordType));

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + apiToken);

        try {
            return httpClient.execute(request, response -> {
                try {
                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    if (response.getCode() != 200) {
                        throw new RuntimeException(new CloudflareException(
                            "Failed to get DNS record. Status: " + response.getCode()
                                + ", Response: " + jsonResponse));
                    }

                    JsonNode root = objectMapper.readTree(jsonResponse);
                    if (!root.path("success").asBoolean()) {
                        throw new RuntimeException(new CloudflareException(
                            "API request failed: " + jsonResponse));
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
            throw new CloudflareException("Error getting DNS record for " + recordName, e);
        }
    }

    public void updateDnsRecord(String apiToken, String zoneId, String id,
                                String name, String type, String content) throws CloudflareException {
        String url = String.format("%s/zones/%s/dns_records/%s", API_BASE, encode(zoneId), encode(id));

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", type);
        payload.put("name", name);
        payload.put("content", content);

        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpPut request = new HttpPut(url);
            request.setHeader("Authorization", "Bearer " + apiToken);
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            httpClient.execute(request, response -> {
                try {
                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    if (response.getCode() != 200) {
                        throw new RuntimeException(new CloudflareException(
                            "Failed to update DNS record. Status: " + response.getCode()
                                + ", Response: " + jsonResponse));
                    }

                    JsonNode root = objectMapper.readTree(jsonResponse);
                    if (!root.path("success").asBoolean()) {
                        throw new RuntimeException(new CloudflareException(
                            "API request failed: " + jsonResponse));
                    }

                    logger.info("Successfully updated DNS record {} to {}", name, content);
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException("Error processing response", e);
                }
            });
        } catch (Exception e) {
            throw new CloudflareException("Error updating DNS record " + name, e);
        }
    }

    /**
     * Creates a new DNS record in the given zone. Used when the configured
     * record does not yet exist. TTL is left automatic and the record is
     * unproxied (so the real IP is served), which is what dynamic DNS needs.
     */
    public void createDnsRecord(String apiToken, String zoneId,
                                String name, String type, String content) throws CloudflareException {
        String url = String.format("%s/zones/%s/dns_records", API_BASE, encode(zoneId));

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", type);
        payload.put("name", name);
        payload.put("content", content);
        payload.put("ttl", 1);          // 1 = automatic
        payload.put("proxied", false);

        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpPost request = new HttpPost(url);
            request.setHeader("Authorization", "Bearer " + apiToken);
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            httpClient.execute(request, response -> {
                try {
                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    if (response.getCode() != 200) {
                        throw new RuntimeException(new CloudflareException(
                            "Failed to create DNS record. Status: " + response.getCode()
                                + ", Response: " + jsonResponse));
                    }

                    JsonNode root = objectMapper.readTree(jsonResponse);
                    if (!root.path("success").asBoolean()) {
                        throw new RuntimeException(new CloudflareException(
                            "API request failed: " + jsonResponse));
                    }

                    logger.info("Successfully created DNS record {} ({}) -> {}", name, type, content);
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException("Error processing response", e);
                }
            });
        } catch (Exception e) {
            throw new CloudflareException("Error creating DNS record " + name, e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
