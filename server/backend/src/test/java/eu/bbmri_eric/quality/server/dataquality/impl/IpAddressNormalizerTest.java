package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class IpAddressNormalizerTest {

  @Test
  void normalize_ipv4_returnsSameValue() {
    assertEquals("192.168.1.1", IpAddressNormalizer.normalize("192.168.1.1"));
  }

  @Test
  void normalize_surroundingWhitespace_isTrimmed() {
    assertEquals("10.0.0.5", IpAddressNormalizer.normalize(" 10.0.0.5 "));
  }

  @Test
  void normalize_ipv4MappedIpv6_returnsEmbeddedIpv4() {
    assertEquals("10.0.0.5", IpAddressNormalizer.normalize("::ffff:10.0.0.5"));
    assertEquals("10.0.0.5", IpAddressNormalizer.normalize("0:0:0:0:0:ffff:10.0.0.5"));
  }

  @Test
  void normalize_uppercaseIpv4MappedIpv6_returnsEmbeddedIpv4() {
    assertEquals("10.0.0.5", IpAddressNormalizer.normalize("::FFFF:10.0.0.5"));
  }

  @Test
  void normalize_sameIpv6InCompressedAndExpandedForm_returnsIdenticalValue() {
    assertEquals(
        IpAddressNormalizer.normalize("fe80::1"),
        IpAddressNormalizer.normalize("FE80:0:0:0:0:0:0:1"));
  }

  @Test
  void normalize_uppercaseIpv6_returnsLowercasedValue() {
    String normalized = IpAddressNormalizer.normalize("FE80::1");
    assertEquals(normalized.toLowerCase(Locale.ROOT), normalized);
  }

  @Test
  void normalize_ipv6WithScopeId_stripsScope() {
    assertEquals(
        IpAddressNormalizer.normalize("fe80::1"), IpAddressNormalizer.normalize("fe80::1%eth0"));
  }

  @Test
  void normalize_nullOrBlank_returnsNull() {
    assertNull(IpAddressNormalizer.normalize(null));
    assertNull(IpAddressNormalizer.normalize(""));
    assertNull(IpAddressNormalizer.normalize("   "));
  }

  @Test
  void normalize_garbage_returnsNullWithoutDns() {
    assertNull(IpAddressNormalizer.normalize("unknown"));
    assertNull(IpAddressNormalizer.normalize("not-an-ip"));
    assertNull(IpAddressNormalizer.normalize("192.168.1.256"));
    assertNull(IpAddressNormalizer.normalize("example.com"));
  }
}
