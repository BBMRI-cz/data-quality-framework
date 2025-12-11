package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;

/**
 * Entity representing a category for grouping quality checks.
 *
 * <p>Each category is uniquely identified by an auto-generated ID and has a unique name.
 * Categories can be visually distinguished by their color.
 */
@Entity
public class Category {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(unique = true)
  private String name;

  @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color code (e.g., #FF5733)")
  private String colorHex;

  /** Default constructor for JPA. */
  protected Category() {}

  /**
   * Creates a new category.
   *
   * @param name the unique name of the category
   * @param colorHex the hex color code for visual representation (e.g., #FF5733)
   */
  public Category(String name, String colorHex) {
    this.name = name;
    this.colorHex = colorHex;
  }

  /**
   * Gets the unique identifier of this category.
   *
   * @return the category ID
   */
  public Long getId() {
    return id;
  }

  /**
   * Gets the name of this category.
   *
   * @return the category name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of this category.
   *
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the hex color code of this category.
   *
   * @return the color hex code
   */
  public String getColorHex() {
    return colorHex;
  }

  /**
   * Sets the hex color code of this category.
   *
   * @param colorHex the color hex code to set (e.g., #FF5733)
   */
  public void setColorHex(String colorHex) {
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

  @Override
  public String toString() {
    return "Category{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", colorHex='" + colorHex + '\'' +
        '}';
  }
}

