package eu.bbmri_eric.quality.agent.system;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

public class PagingUrlInterceptor implements IClientInterceptor {
  private final int mappedPort;

  public PagingUrlInterceptor(int mappedPort) {
    this.mappedPort = mappedPort;
  }

  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    String originalUri = theRequest.getUri();
    if (originalUri.contains(":8080")) {
      String newUri = originalUri.replace(":8080", ":" + mappedPort);
      theRequest.setUri(newUri);
    }
  }

  public void interceptResponse(IHttpResponse theResponse) {}
}
