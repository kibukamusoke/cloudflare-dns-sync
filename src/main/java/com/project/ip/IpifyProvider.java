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
    public InetAddress getCurrentIpAddress() throws IpLookupException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_URL);
            
            return client.execute(request, response -> {
                try {
                    if (response.getCode() != 200) {
                        throw new RuntimeException(new IpLookupException(
                            "Failed to get IP address from ipify. Status code: " + response.getCode()));
                    }
                    
                    String ip = EntityUtils.toString(response.getEntity()).trim();
                    logger.debug("Retrieved IP address from ipify: {}", ip);
                    try {
                        return InetAddress.getByName(ip);
                    } catch (IOException e) {
                        throw new RuntimeException(new IpLookupException("Invalid IP address format", e));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(new IpLookupException("Error reading response", e));
                }
            });
        } catch (Exception e) {
            if (e.getCause() instanceof IpLookupException) {
                throw (IpLookupException) e.getCause();
            }
            throw new IpLookupException("Error getting IP address from ipify", e);
        }
    }

    @Override
    public String getProviderName() {
        return "ipify";
    }
} 