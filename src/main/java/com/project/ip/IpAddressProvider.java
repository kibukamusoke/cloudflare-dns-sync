package com.project.ip;

public interface IpAddressProvider {
    /**
     * Retrieves the current public IP address for the given protocol version.
     *
     * @param version the IP version to look up (IPv4 or IPv6)
     * @return The current public IP address
     * @throws IpLookupException if the IP address lookup fails or the provider
     *                           does not support the requested version
     */
    String getCurrentIpAddress(IpVersion version) throws IpLookupException;

    /**
     * Returns the name of this IP address provider.
     *
     * @return The provider name
     */
    String getProviderName();
}
