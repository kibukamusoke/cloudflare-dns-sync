package com.project.ip;

import java.net.InetAddress;

public interface IpAddressProvider {
    /**
     * Retrieves the current public IP address.
     *
     * @return The current public IP address
     * @throws IpLookupException if the IP address lookup fails
     */
    InetAddress getCurrentIpAddress() throws IpLookupException;
    
    /**
     * Returns the name of this IP address provider.
     *
     * @return The provider name
     */
    String getProviderName();
} 