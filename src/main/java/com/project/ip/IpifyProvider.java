package com.project.ip;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

public class IpifyProvider implements IpAddressProvider {
    private static final Logger logger = LoggerFactory.getLogger(IpifyProvider.class);
    private static final String API_URL = "https://api.ipify.org";

    @Override
    public String getCurrentIpAddress() throws IpLookupException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_URL);
            
            return client.execute(request, response -> {
                int status = response.getCode();
                if (status != 200) {
                    throw new RuntimeException("API request failed with status: " + status);
                }
                
                String ip = EntityUtils.toString(response.getEntity()).trim();
                logger.debug("Retrieved IP address from ipify: {}", ip);
                return ip;
            });
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().startsWith("API request failed")) {
                throw new IpLookupException(e.getMessage());
            }
            throw new IpLookupException("Failed to get IP address", e);
        }
    }

    @Override
    public String getProviderName() {
        return "ipify";
    }
} 