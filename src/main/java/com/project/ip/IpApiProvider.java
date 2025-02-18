package com.project.ip;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpApiProvider implements IpAddressProvider {
    private static final Logger logger = LoggerFactory.getLogger(IpApiProvider.class);
    private static final String API_URL = "http://ip-api.com/line/?fields=query";

    @Override
    public String getCurrentIpAddress() throws IpLookupException {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(5))
            .setResponseTimeout(Timeout.ofSeconds(5))
            .build();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_URL);
            request.setConfig(config);
            logger.debug("Requesting IP from ip-api.com...");
            
            return client.execute(request, response -> {
                int status = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity()).trim();
                
                if (status != 200) {
                    logger.warn("IP-API returned status {}: {}", status, responseBody);
                    throw new RuntimeException("API request failed with status " + status + ": " + responseBody);
                }
                
                if (!responseBody.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                    logger.warn("Invalid IP format received: {}", responseBody);
                    throw new RuntimeException("Invalid IP format received: " + responseBody);
                }

                logger.debug("Retrieved IP address from ip-api.com: {}", responseBody);
                return responseBody;
            });
        } catch (Exception e) {
            logger.debug("Error details:", e);
            throw new IpLookupException("Failed to get IP address: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "ip-api.com";
    }
} 