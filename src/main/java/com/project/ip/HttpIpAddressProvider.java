package com.project.ip;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for providers that fetch the public IP from a plain-text HTTPS endpoint.
 * Subclasses supply the endpoint URL per IP version; this class handles the
 * request, status/format checks and exception wrapping.
 */
public abstract class HttpIpAddressProvider implements IpAddressProvider {
    private static final Logger logger = LoggerFactory.getLogger(HttpIpAddressProvider.class);
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(5))
        .setResponseTimeout(Timeout.ofSeconds(5))
        .build();

    private final CloseableHttpClient client;

    protected HttpIpAddressProvider(CloseableHttpClient client) {
        this.client = client;
    }

    /**
     * @return the endpoint URL for the given version, or {@code null} if this
     *         provider does not support it.
     */
    protected abstract String endpointFor(IpVersion version);

    @Override
    public String getCurrentIpAddress(IpVersion version) throws IpLookupException {
        String url = endpointFor(version);
        if (url == null) {
            throw new IpLookupException(getProviderName() + " does not support " + version);
        }

        HttpGet request = new HttpGet(url);
        request.setConfig(REQUEST_CONFIG);
        logger.debug("Requesting {} address from {}...", version, getProviderName());

        try {
            return client.execute(request, response -> {
                int status = response.getCode();
                String body = EntityUtils.toString(response.getEntity()).trim();

                if (status != 200) {
                    throw new RuntimeException(
                        "API request failed with status " + status + ": " + body);
                }
                if (!IpValidator.isValid(version, body)) {
                    throw new RuntimeException("Invalid " + version + " address received: " + body);
                }
                logger.debug("Retrieved {} address from {}: {}", version, getProviderName(), body);
                return body;
            });
        } catch (Exception e) {
            logger.debug("Error details:", e);
            throw new IpLookupException(
                "Failed to get IP address from " + getProviderName() + ": " + e.getMessage(), e);
        }
    }
}
