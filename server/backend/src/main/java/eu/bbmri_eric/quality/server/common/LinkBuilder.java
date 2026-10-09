package eu.bbmri_eric.quality.server.common;

import eu.bbmri_eric.quality.server.common.dto.FilterDTO;
import eu.bbmri_eric.quality.server.common.dto.PageResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
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
  private static final Logger log = LoggerFactory.getLogger(LinkBuilder.class);

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
   * <p>All non-null fields of the given filter (including subclasses of {@link FilterDTO}) are
   * serialized into every link, so paging preserves domain-specific filters.
   *
   * @param baseUri base uri of the request, e.g. /api/resources
   * @param filterDTO filter DTO containing the filter parameters
   * @param pageMetadata page metadata
   * @return list of page links
   */
  public static List<Link> getPageLinks(
      URI baseUri, FilterDTO filterDTO, PagedModel.PageMetadata pageMetadata) {
    List<Link> links = new ArrayList<>();

    int currentPage = (int) pageMetadata.getNumber();
    int originalPage = filterDTO.getPage();
    try {
      filterDTO.setPage(currentPage);
      links.add(
          Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.CURRENT));
      if (currentPage > 0) {
        filterDTO.setPage(0);
        links.add(
            Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.FIRST));
        filterDTO.setPage(currentPage - 1);
        links.add(
            Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.PREVIOUS));
      }

      long lastPage = Math.max(pageMetadata.getTotalPages() - 1, 0);
      if (currentPage < lastPage) {
        filterDTO.setPage(currentPage + 1);
        links.add(
            Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.NEXT));
        filterDTO.setPage((int) lastPage);
        links.add(
            Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.LAST));
      }
    } finally {
      filterDTO.setPage(originalPage);
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
    Class<?> type = filterDTO.getClass();
    while (type != null && type != Object.class) {
      for (Field field : type.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          addQueryParam(queryParams, field, filterDTO);
        }
      }
      type = type.getSuperclass();
    }
    return queryParams;
  }

  private static void addQueryParam(
      MultiValueMap<String, String> queryParams, Field field, FilterDTO filterDTO) {
    try {
      field.setAccessible(true);
      Object value = field.get(filterDTO);
      if (value == null) {
        return;
      }
      if (value instanceof Collection<?> collection) {
        for (Object item : collection) {
          queryParams.add(field.getName(), String.valueOf(item));
        }
      } else {
        queryParams.add(field.getName(), String.valueOf(value));
      }
    } catch (IllegalAccessException e) {
      log.error("Error while getting query param '{}'", field.getName(), e);
    }
  }
}
