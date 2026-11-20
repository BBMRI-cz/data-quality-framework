package eu.bbmri_eric.quality.agent.config;

import java.time.Duration;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
class RestClientConfig {

  @Bean
  RestTemplate restTemplate() {
    // To support PATCH method
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();
    return new RestTemplate(requestFactory);
  }

  /**
   * RestTemplate dedicated to central-server communication with bounded connect and response
   * timeouts, so a hung or unreachable server fails the request instead of blocking the calling
   * thread indefinitely (e.g. registration, login, status checks, report sending).
   */
  @Bean
  RestTemplate centralServerRestTemplate(
      @Value("${agent.central-server.connect-timeout:5s}") Duration connectTimeout,
      @Value("${agent.central-server.read-timeout:10s}") Duration readTimeout) {
    // HttpClient 5 has no connect/response timeout by default; the connection request timeout is
    // bounded with the connect value to avoid waiting indefinitely for a pooled connection.
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
            .setResponseTimeout(Timeout.ofMilliseconds(readTimeout.toMillis()))
            .build();
    CloseableHttpClient httpClient =
        HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
    return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
  }
}
