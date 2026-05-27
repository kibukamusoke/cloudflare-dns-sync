package com.project.ip;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

/**
 * Looks up the public IP via icanhazip.com (Cloudflare) over HTTPS. Supports
 * both IPv4 (ipv4.icanhazip.com) and IPv6 (ipv6.icanhazip.com). Used as a
 * fallback when {@link IpifyProvider} is unreachable.
 */
public class IcanhazipProvider extends HttpIpAddressProvider {
    public IcanhazipProvider(CloseableHttpClient client) {
        super(client);
    }

    @Override
    protected String endpointFor(IpVersion version) {
        switch (version) {
            case V4:
                return "https://ipv4.icanhazip.com";
            case V6:
                return "https://ipv6.icanhazip.com";
            default:
                return null;
        }
    }

    @Override
    public String getProviderName() {
        return "icanhazip";
    }
}
