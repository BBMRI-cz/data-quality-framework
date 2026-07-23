package eu.bbmri_eric.quality.agent.dataquality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity representing a category for grouping quality checks.
 *
 * <p>Each category is uniquely identified by an auto-generated ID and has a unique name. Categories
 * can be visually distinguished by their color.
 */
@Entity
@Getter
@Setter
@ToString
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(unique = true)
  private String name;

  @Pattern(
      regexp = "^#[0-9A-Fa-f]{6}$",
      message = "Color must be a valid hex color code (e.g., #FF5733)")
  private String colorHex;

  protected Category() {}

  public Category(String name, String colorHex) {
    this.name = name;
    this.colorHex = colorHex;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Category category = (Category) o;
    return Objects.equals(id, category.id) && Objects.equals(name, category.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}
