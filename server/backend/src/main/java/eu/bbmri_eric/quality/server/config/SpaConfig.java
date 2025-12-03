package eu.bbmri_eric.quality.server.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Configuration for serving Single Page Application (SPA) resources.
 *
 * <p>This configuration handles routing for a Vue.js (or similar) SPA by:
 *
 * <ul>
 *   <li>Serving static assets (JS, CSS, images) from the classpath:/static/ directory
 *   <li>Forwarding all non-API and non-static-file requests to index.html for client-side routing
 *   <li>Allowing API endpoints to be handled by Spring controllers
 * </ul>
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

  private static final String STATIC_RESOURCE_LOCATION = "classpath:/static/";
  private static final String INDEX_HTML_PATH = "/static/index.html";
  private static final String API_PATH_PREFIX = "api/";

  /**
   * Configures resource handlers for serving SPA static resources and handling client-side routes.
   *
   * @param registry the resource handler registry
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations(STATIC_RESOURCE_LOCATION)
        .resourceChain(false)
        .addResolver(new SpaPathResourceResolver());
  }

  /**
   * Custom resource resolver that serves static files or falls back to index.html for SPA routes.
   */
  private static class SpaPathResourceResolver extends PathResourceResolver {

    /**
     * Resolves the requested resource path to either a static file or the SPA index.html.
     *
     * @param resourcePath the path of the requested resource
     * @param location the base location to resolve from
     * @return the resolved resource, or null if it's an API path
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
      if (isApiPath(resourcePath)) {
        return null;
      }
      Resource requestedResource = location.createRelative(resourcePath);
      if (isExistingStaticResource(requestedResource)) {
        return requestedResource;
      }
      return getFallbackIndexHtml();
    }

    /**
     * Checks if the requested path is an API endpoint.
     *
     * @param resourcePath the resource path to check
     * @return true if the path starts with the API prefix
     */
    private boolean isApiPath(String resourcePath) {
      return resourcePath.startsWith(API_PATH_PREFIX);
    }

    /**
     * Checks if the resource exists and is readable as a static file.
     *
     * @param resource the resource to check
     * @return true if the resource exists and can be read
     */
    private boolean isExistingStaticResource(Resource resource) {
      return resource.exists() && resource.isReadable();
    }

    /**
     * Returns the fallback index.html resource for SPA client-side routing.
     *
     * @return the index.html resource
     */
    private Resource getFallbackIndexHtml() {
      return new ClassPathResource(INDEX_HTML_PATH);
    }
  }
}
