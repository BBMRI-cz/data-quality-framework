package eu.bbmri_eric.quality.server.user.dto;

import eu.bbmri_eric.quality.server.common.dto.FilterDTO;
import eu.bbmri_eric.quality.server.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Filter DTO for querying users.
 *
 * <p>Extends the common {@link FilterDTO} with user-specific filters: a free-text search matched
 * against username and subject ID, and role-based filtering.
 */
@Schema(description = "Filter parameters for listing users")
public class UserFilterDTO extends FilterDTO {

  @Schema(
      description = "Free-text search matched against username and subject ID (case-insensitive)",
      example = "john")
  private String search;

  @Schema(
      description =
          "Only include users having any of the given roles (comma-separated, case-insensitive)",
      example = "human_user,admin")
  private Set<UserRole> roles = new HashSet<>();

  /** Default constructor. */
  public UserFilterDTO() {}

  /**
   * Constructs a filter with pagination/sorting parameters only.
   *
   * @param page the page number (zero-based)
   * @param size the page size
   * @param sort the sort property
   * @param order the sort order
   */
  public UserFilterDTO(int page, int size, String sort, SortOrder order) {
    super(page, size, sort, order);
  }

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public Set<UserRole> getRoles() {
    return roles;
  }

  public void setRoles(Set<UserRole> roles) {
    this.roles = roles == null ? new HashSet<>() : roles;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    UserFilterDTO that = (UserFilterDTO) o;
    return Objects.equals(search, that.search) && Objects.equals(roles, that.roles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), search, roles);
  }

  @Override
  public String toString() {
    return "UserFilterDTO{"
        + "search='"
        + search
        + '\''
        + ", roles="
        + roles
        + "} "
        + super.toString();
  }
}
