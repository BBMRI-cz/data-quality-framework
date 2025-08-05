package eu.bbmri_eric.quality.agent.report;

import jakarta.persistence.*;

@Entity
class Patient {
  @Id private String id;

  public Patient() {}

  public Patient(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
