package com.project.ip;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

/**
 * Looks up the public IP via ipify.org over HTTPS. Supports both IPv4
 * (api.ipify.org) and IPv6 (api6.ipify.org).
 */
public class IpifyProvider extends HttpIpAddressProvider {
    public IpifyProvider(CloseableHttpClient client) {
        super(client);
    }

    @Override
    protected String endpointFor(IpVersion version) {
        switch (version) {
            case V4:
                return "https://api.ipify.org?format=text";
            case V6:
                return "https://api6.ipify.org?format=text";
            default:
                return null;
        }
    }

    @Override
    public String getProviderName() {
        return "ipify";
    }
}
