package com.example.meetingroom.security;

import com.example.meetingroom.config.AdminLoginProperties;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IpWhitelist {

    private final List<IpRule> rules;

    public IpWhitelist(AdminLoginProperties properties) {
        this.rules = properties.getAllowedIps().stream()
            .map(IpRule::parse)
            .toList();
    }

    public boolean isAllowed(String remoteAddress) {
        try {
            InetAddress address = InetAddress.getByName(remoteAddress);
            return rules.stream().anyMatch(rule -> rule.matches(address));
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private record IpRule(BigInteger network, BigInteger mask, int byteLength) {

        static IpRule parse(String value) {
            String[] parts = value.trim().split("/", 2);
            try {
                InetAddress address = InetAddress.getByName(parts[0]);
                int byteLength = address.getAddress().length;
                int bitLength = byteLength * 8;
                int prefixLength = parts.length == 2 ? Integer.parseInt(parts[1]) : bitLength;
                if (prefixLength < 0 || prefixLength > bitLength) {
                    throw new IllegalArgumentException("Invalid IP whitelist prefix: " + value);
                }

                BigInteger mask = mask(bitLength, prefixLength);
                BigInteger network = toBigInteger(address).and(mask);
                return new IpRule(network, mask, byteLength);
            } catch (UnknownHostException | NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid IP whitelist entry: " + value, exception);
            }
        }

        boolean matches(InetAddress address) {
            return address.getAddress().length == byteLength
                && toBigInteger(address).and(mask).equals(network);
        }

        private static BigInteger toBigInteger(InetAddress address) {
            return new BigInteger(1, address.getAddress());
        }

        private static BigInteger mask(int bitLength, int prefixLength) {
            if (prefixLength == 0) {
                return BigInteger.ZERO;
            }
            return BigInteger.ONE.shiftLeft(prefixLength)
                .subtract(BigInteger.ONE)
                .shiftLeft(bitLength - prefixLength);
        }
    }
}
