package eu.bbmri_eric.quality.server.dataquality.impl;

import java.net.InetAddress;
import java.util.Locale;

/**
 * Normalizes IP address text so different spellings of the same address (e.g. the IPv4-mapped IPv6
 * form {@code ::ffff:10.0.0.5} produced by dual-stack servlet containers) compare equal.
 *
 * <p>Parsing is literal-only ({@link InetAddress#ofLiteral(String)}) so hostnames never trigger a
 * DNS lookup, and normalization never throws: unparsable input yields {@code null}.
 */
final class IpAddressNormalizer {

  private static final int IPV6_BYTE_LENGTH = 16;
  private static final int IPV4_MAPPED_PREFIX_LENGTH = 10;
  private static final int IPV4_MAPPED_SUFFIX_OFFSET = 12;
  private static final char SCOPE_SEPARATOR = '%';

  private IpAddressNormalizer() {}

  static String normalize(String ipAddress) {
    if (ipAddress == null || ipAddress.isBlank()) {
      return null;
    }
    String literal = ipAddress.trim();
    int scopeSeparator = literal.indexOf(SCOPE_SEPARATOR);
    if (scopeSeparator >= 0) {
      literal = literal.substring(0, scopeSeparator);
    }
    try {
      var address = InetAddress.ofLiteral(literal);
      byte[] bytes = address.getAddress();
      if (bytes.length == IPV6_BYTE_LENGTH && isIpv4Mapped(bytes)) {
        return toDottedQuad(bytes);
      }
      return address.getHostAddress().toLowerCase(Locale.ROOT);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static boolean isIpv4Mapped(byte[] bytes) {
    for (int i = 0; i < IPV4_MAPPED_PREFIX_LENGTH; i++) {
      if (bytes[i] != 0) {
        return false;
      }
    }
    return (bytes[IPV4_MAPPED_PREFIX_LENGTH] & 0xFF) == 0xFF
        && (bytes[IPV4_MAPPED_PREFIX_LENGTH + 1] & 0xFF) == 0xFF;
  }

  private static String toDottedQuad(byte[] bytes) {
    return "%d.%d.%d.%d"
        .formatted(
            bytes[IPV4_MAPPED_SUFFIX_OFFSET] & 0xFF,
            bytes[IPV4_MAPPED_SUFFIX_OFFSET + 1] & 0xFF,
            bytes[IPV4_MAPPED_SUFFIX_OFFSET + 2] & 0xFF,
            bytes[IPV4_MAPPED_SUFFIX_OFFSET + 3] & 0xFF);
  }
}
