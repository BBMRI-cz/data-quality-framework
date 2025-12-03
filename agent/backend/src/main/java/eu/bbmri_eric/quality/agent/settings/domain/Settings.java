package eu.bbmri_eric.quality.agent.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Settings {

  @Id
  @NotNull
  @Column(name = "setting_name")
  private String name;

  @NotNull
  @Column(name = "setting_value")
  private String value;

  protected Settings() {}

  public Settings(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Settings settings = (Settings) o;
    return Objects.equals(name, settings.name) && Objects.equals(value, settings.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }
}
