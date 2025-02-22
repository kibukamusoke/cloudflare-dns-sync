package com.project.ip;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.net.InetAddress;

public class IpifyProvider implements IpAddressProvider {
    private static final Logger logger = LoggerFactory.getLogger(IpifyProvider.class);
    private static final String API_URL = "https://api.ipify.org?format=text";

    @Override
    public String getCurrentIpAddress() throws IpLookupException {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(5))
            .setResponseTimeout(Timeout.ofSeconds(5))
            .build();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_URL);
            request.setConfig(config);
            
            logger.debug("Requesting IP from ipify.org...");
            
            return client.execute(request, response -> {
                int status = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity()).trim();
                
                if (status != 200) {
                    logger.warn("Ipify API returned status {}: {}", status, responseBody);
                    throw new RuntimeException("API request failed with status " + status + ": " + responseBody);
                }
                
                String ip = responseBody;
                if (!ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                    logger.warn("Invalid IP format received: {}", ip);
                    throw new RuntimeException("Invalid IP format received: " + ip);
                }
                
                logger.debug("Retrieved IP address from ipify: {}", ip);
                return ip;
            });
        } catch (Exception e) {
            logger.debug("Error details:", e);
            if (e.getMessage() != null && e.getMessage().startsWith("API request failed")) {
                throw new IpLookupException(e.getMessage());
            }
            throw new IpLookupException("Failed to get IP address: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "ipify";
    }
} 