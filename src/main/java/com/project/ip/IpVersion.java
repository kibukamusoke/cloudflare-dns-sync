package com.project.ip;

/**
 * IP protocol version, derived from the Cloudflare record type.
 * {@code AAAA} records need an IPv6 address; everything else uses IPv4.
 */
public enum IpVersion {
    V4,
    V6;

    public static IpVersion fromRecordType(String recordType) {
        return "AAAA".equalsIgnoreCase(recordType) ? V6 : V4;
    }
}
