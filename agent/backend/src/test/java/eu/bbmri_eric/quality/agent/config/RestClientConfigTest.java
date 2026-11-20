package eu.bbmri_eric.quality.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies that the dedicated central-server {@link RestTemplate} carries bounded connect/read
 * timeouts so agent to central-server communication fails fast instead of hanging indefinitely.
 */
@SpringBootTest(classes = RestClientConfig.class)
class RestClientConfigTest {

  @Autowired
  @Qualifier("centralServerRestTemplate")
  private RestTemplate centralServerRestTemplate;

  @Test
  void centralServerRestTemplate_withoutProperties_usesDefaultTimeouts() {
    RequestConfig requestConfig = defaultRequestConfig(centralServerRestTemplate);
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(Timeout.ofSeconds(5));
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.ofSeconds(5));
    assertThat(requestConfig.getResponseTimeout()).isEqualTo(Timeout.ofSeconds(10));
  }

  @Nested
  @SpringBootTest(
      classes = RestClientConfig.class,
      properties = {
        "agent.central-server.connect-timeout=2s",
        "agent.central-server.read-timeout=3s"
      })
  class WithConfiguredTimeouts {

    @Autowired
    @Qualifier("centralServerRestTemplate")
    private RestTemplate centralServerRestTemplate;

    @Test
    void centralServerRestTemplate_withProperties_appliesConfiguredTimeouts() {
      RequestConfig requestConfig = defaultRequestConfig(centralServerRestTemplate);
      assertThat(requestConfig.getConnectTimeout()).isEqualTo(Timeout.ofSeconds(2));
      assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.ofSeconds(2));
      assertThat(requestConfig.getResponseTimeout()).isEqualTo(Timeout.ofSeconds(3));
    }
  }

  /**
   * Extracts the default {@link RequestConfig} of the Apache HttpClient 5 client backing the given
   * {@link RestTemplate}. The {@code defaultConfig} field is an httpclient5 implementation detail,
   * stable across the versions managed by Spring Boot; if a future upgrade renames it, this helper
   * fails loudly.
   */
  private static RequestConfig defaultRequestConfig(RestTemplate restTemplate) {
    HttpComponentsClientHttpRequestFactory requestFactory =
        (HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory();
    CloseableHttpClient httpClient = (CloseableHttpClient) requestFactory.getHttpClient();
    return (RequestConfig) ReflectionTestUtils.getField(httpClient, "defaultConfig");
  }
}
