package eu.bbmri_eric.quality.server.common;

import eu.bbmri_eric.quality.server.common.dto.FilterDTO;
import eu.bbmri_eric.quality.server.common.dto.PageResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/** Utility for building pagination links using Spring HATEOAS link relations. */
public final class LinkBuilder {
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

    int currentPage = (int) pageMetadata.getNumber();
    links.add(
        Link.of(createBaseUriBuilder(baseUri, copyWithPage(filterDTO, currentPage)))
            .withRel(IanaLinkRelations.CURRENT));
    if (currentPage > 0) {
      links.add(
          Link.of(createBaseUriBuilder(baseUri, copyWithPage(filterDTO, 0)))
              .withRel(IanaLinkRelations.FIRST));
      links.add(
          Link.of(createBaseUriBuilder(baseUri, copyWithPage(filterDTO, currentPage - 1)))
              .withRel(IanaLinkRelations.PREVIOUS));
    }

    long lastPage = Math.max(pageMetadata.getTotalPages() - 1, 0);
    if (currentPage < lastPage) {
      links.add(
          Link.of(createBaseUriBuilder(baseUri, copyWithPage(filterDTO, currentPage + 1)))
              .withRel(IanaLinkRelations.NEXT));
      links.add(
          Link.of(createBaseUriBuilder(baseUri, copyWithPage(filterDTO, (int) lastPage)))
              .withRel(IanaLinkRelations.LAST));
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

    queryParams.add("page", String.valueOf(filterDTO.getPage()));
    queryParams.add("size", String.valueOf(filterDTO.getSize()));
    if (filterDTO.getSort() != null && !filterDTO.getSort().isBlank()) {
      queryParams.add("sort", filterDTO.getSort());
    }
    if (filterDTO.getOrder() != null) {
      queryParams.add("order", filterDTO.getOrder().name());
    }

    return queryParams;
  }

  private static FilterDTO copyWithPage(FilterDTO filterDTO, int page) {
    return new FilterDTO(page, filterDTO.getSize(), filterDTO.getSort(), filterDTO.getOrder());
  }
}
