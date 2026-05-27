package com.project.ip;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * Strict validation of IP literals returned by the lookup providers.
 * IPv4 octets are range-checked; IPv6 is validated via {@link InetAddress}
 * after a character-set guard so a malformed value can never trigger a DNS
 * lookup.
 */
public final class IpValidator {
    private static final Pattern V4 = Pattern.compile(
        "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Pattern V6_CHARS = Pattern.compile("^[0-9A-Fa-f:.]+$");

    private IpValidator() {
    }

    public static boolean isValid(IpVersion version, String ip) {
        if (ip == null) {
            return false;
        }
        ip = ip.trim();
        switch (version) {
            case V4:
                return V4.matcher(ip).matches();
            case V6:
                if (ip.indexOf(':') < 0 || !V6_CHARS.matcher(ip).matches()) {
                    return false;
                }
                try {
                    return InetAddress.getByName(ip) instanceof Inet6Address;
                } catch (Exception e) {
                    return false;
                }
            default:
                return false;
        }
    }
}
