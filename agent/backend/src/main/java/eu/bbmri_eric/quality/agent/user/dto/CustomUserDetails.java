package eu.bbmri_eric.quality.agent.user.dto;

import eu.bbmri_eric.quality.agent.user.domain.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

  public static final String DEFAULT_ADMIN_PASS =
      "$argon2id$v=19$m=19456,t=2,p=1$SQGK8wXpQw5b+qjuq/Ih1A$WP87YsUIErq6O+7rMk65U0cH4OHBRdrnM3yIG50gwpE";
  private final UserDTO user;
  private final String password;

  public CustomUserDetails(User user) {
    this.user = new UserDTO(user.getUsername(), user.getId());
    this.password = user.getPassword();
    this.user.setDefaultPassword(this.password.equals(DEFAULT_ADMIN_PASS));
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  public UserDTO getUser() {
    return user;
  }
}
