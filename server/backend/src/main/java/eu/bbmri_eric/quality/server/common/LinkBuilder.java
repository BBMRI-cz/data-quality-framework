package eu.bbmri_eric.quality.server.common;

import eu.bbmri_eric.quality.server.common.dto.FilterDTO;
import eu.bbmri_eric.quality.server.common.dto.PageResponse;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/** Utility for building pagination links using Spring HATEOAS link relations. */
public final class LinkBuilder {
  private static final Logger logger = LoggerFactory.getLogger(LinkBuilder.class);

  private LinkBuilder() {}

  /**
   * Get page links for HATEOAS response models.
   *
   * @param baseUri base uri of the request, e.g. /api/resources
   * @param filterDTO filter DTO containing the filter parameters
   * @param pageResponse page response containing metadata
   * @return list of page links
   */
  public static List<Link> getPageLinks(
      URI baseUri, FilterDTO filterDTO, PageResponse<?> pageResponse) {
    PagedModel.PageMetadata pageMetadata =
        new PagedModel.PageMetadata(
            pageResponse.getSize(),
            pageResponse.getPage(),
            pageResponse.getTotalElements(),
            pageResponse.getTotalPages());
    return getPageLinks(baseUri, filterDTO, pageMetadata);
  }

  /**
   * Get page links for HATEOAS response models.
   *
   * @param baseUri base uri of the request, e.g. /api/resources
   * @param filterDTO filter DTO containing the filter parameters
   * @param pageMetadata page metadata
   * @return list of page links
   */
  public static List<Link> getPageLinks(
      URI baseUri, FilterDTO filterDTO, PagedModel.PageMetadata pageMetadata) {
    List<Link> links = new ArrayList<>();

    links.add(Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.CURRENT));
    int currentPage = filterDTO.getPage();
    if (pageMetadata.getNumber() > 0) {
      filterDTO.setPage(0);
      links.add(Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.FIRST));
      filterDTO.setPage(currentPage - 1);
      links.add(
          Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.PREVIOUS));
    }

    if (pageMetadata.getNumber() < pageMetadata.getTotalPages() - 1) {
      filterDTO.setPage(currentPage + 1);
      links.add(Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.NEXT));
      filterDTO.setPage((int) pageMetadata.getTotalPages() - 1);
      links.add(Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.LAST));
    }

    return links;
  }

  public static String createBaseUriBuilder(URI baseUri, FilterDTO filterDTO) {
    return UriComponentsBuilder.fromUri(baseUri)
        .queryParams(getQueryParams(filterDTO))
        .build()
        .toString();
  }

  private static MultiValueMap<String, String> getQueryParams(FilterDTO filterDTO) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Field[] fields = filterDTO.getClass().getDeclaredFields();

    for (Field field : fields) {
      try {
        field.setAccessible(true);
        Object value = field.get(filterDTO);
        if (value != null) {
          queryParams.add(field.getName(), String.valueOf(value));
        }
      } catch (IllegalAccessException e) {
        logger.error("Error while getting query params", e);
      }
    }

    return queryParams;
  }
}
