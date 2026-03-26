package org.narae.service;

import org.narae.config.ApiSecurityProperties;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Component
public class UrlSafetyValidator {
    private final ApiSecurityProperties properties;

    public UrlSafetyValidator(ApiSecurityProperties properties) {
        this.properties = properties;
    }

    public void validatePublicHttpUrl(String value, String fieldName) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCrawlRequestException(fieldName + " must be a valid absolute URL");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new InvalidCrawlRequestException(fieldName + " must use http or https");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidCrawlRequestException(fieldName + " must be an absolute URL");
        }

        int port = uri.getPort();
        if (port != -1 && !properties.getCrawl().getAllowedPorts().contains(port)) {
            throw new InvalidCrawlRequestException(fieldName + " uses a blocked port");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isPrivateOrLocal(address)) {
                    throw new InvalidCrawlRequestException(fieldName + " points to a blocked host");
                }
            }
        } catch (UnknownHostException exception) {
            throw new InvalidCrawlRequestException(fieldName + " host cannot be resolved");
        }
    }

    private boolean isPrivateOrLocal(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address)
                || isCarrierGradeNat(address)
                || isDocumentationRange(address);
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address inet6Address)) {
            return false;
        }
        byte first = inet6Address.getAddress()[0];
        return (first & (byte) 0xfe) == (byte) 0xfc;
    }

    private boolean isCarrierGradeNat(InetAddress address) {
        if (!(address instanceof Inet4Address inet4Address)) {
            return false;
        }
        byte[] bytes = inet4Address.getAddress();
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        return first == 100 && second >= 64 && second <= 127;
    }

    private boolean isDocumentationRange(InetAddress address) {
        if (!(address instanceof Inet4Address inet4Address)) {
            return false;
        }
        byte[] bytes = inet4Address.getAddress();
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);
        return (first == 192 && second == 0 && third == 2)
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113);
    }
}
