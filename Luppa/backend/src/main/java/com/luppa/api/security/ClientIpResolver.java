package com.luppa.api.security;

import com.luppa.api.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Component
public class ClientIpResolver {
    private final List<IpRange> trustedProxyRanges;

    public ClientIpResolver(RateLimitProperties properties) {
        this.trustedProxyRanges = properties.trustedProxyCidrs() == null
                ? List.of()
                : properties.trustedProxyCidrs().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(IpRange::parse)
                .toList();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddress)) {
            return remoteAddress;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddress;
        }
        String candidate = forwardedFor.split(",")[0].trim();
        return candidate.isBlank() ? remoteAddress : candidate;
    }

    private boolean isTrustedProxy(String remoteAddress) {
        return trustedProxyRanges.stream().anyMatch(range -> range.contains(remoteAddress));
    }

    private record IpRange(long network, long mask) {
        static IpRange parse(String value) {
            String[] parts = value.trim().split("/", 2);
            long address = ipv4ToLong(parts[0]);
            int prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : 32;
            if (prefix < 0 || prefix > 32) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value);
            }
            long mask = prefix == 0 ? 0 : 0xFFFFFFFFL << (32 - prefix) & 0xFFFFFFFFL;
            return new IpRange(address & mask, mask);
        }

        boolean contains(String value) {
            try {
                return (ipv4ToLong(value) & mask) == network;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }

        private static long ipv4ToLong(String value) {
            try {
                byte[] bytes = InetAddress.getByName(value).getAddress();
                if (bytes.length != 4) {
                    throw new IllegalArgumentException("Only IPv4 trusted proxy ranges are supported");
                }
                long result = 0;
                for (byte b : bytes) {
                    result = (result << 8) | (b & 0xFF);
                }
                return result;
            } catch (UnknownHostException exception) {
                throw new IllegalArgumentException("Invalid IP address: " + value, exception);
            }
        }
    }
}
