package com.project.ip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpValidatorTest {

    @Test
    void acceptsValidIpv4() {
        assertTrue(IpValidator.isValid(IpVersion.V4, "192.168.1.1"));
        assertTrue(IpValidator.isValid(IpVersion.V4, "8.8.8.8"));
        assertTrue(IpValidator.isValid(IpVersion.V4, "255.255.255.255"));
        assertTrue(IpValidator.isValid(IpVersion.V4, "0.0.0.0"));
    }

    @Test
    void rejectsOutOfRangeIpv4Octets() {
        assertFalse(IpValidator.isValid(IpVersion.V4, "999.999.999.999"));
        assertFalse(IpValidator.isValid(IpVersion.V4, "256.0.0.1"));
        assertFalse(IpValidator.isValid(IpVersion.V4, "1.2.3"));
        assertFalse(IpValidator.isValid(IpVersion.V4, ""));
        assertFalse(IpValidator.isValid(IpVersion.V4, null));
    }

    @Test
    void ipv6IsNotAcceptedAsIpv4AndViceVersa() {
        assertFalse(IpValidator.isValid(IpVersion.V4, "2001:db8::1"));
        assertFalse(IpValidator.isValid(IpVersion.V6, "192.168.1.1"));
    }

    @Test
    void acceptsValidIpv6() {
        assertTrue(IpValidator.isValid(IpVersion.V6, "2001:db8::1"));
        assertTrue(IpValidator.isValid(IpVersion.V6, "::1"));
        assertTrue(IpValidator.isValid(IpVersion.V6, "fe80::1ff:fe23:4567:890a"));
    }

    @Test
    void rejectsMalformedIpv6WithoutDnsLookup() {
        // contains a non-hex letter: must be rejected and must not resolve as a hostname
        assertFalse(IpValidator.isValid(IpVersion.V6, "example.com"));
        assertFalse(IpValidator.isValid(IpVersion.V6, "gggg::1"));
        assertFalse(IpValidator.isValid(IpVersion.V6, "12345"));
    }
}
