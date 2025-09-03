package eu.bbmri_eric.quality.agent.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_account")
class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String username;
  private String password;

  protected User(String username, String password) {
    this.username = username;
    this.password = password;
  }

  protected User() {}

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public Long getId() {
    return id;
  }

  public void setPassword(String pass) {
    this.password = pass;
  }
}
