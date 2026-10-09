package eu.bbmri_eric.quality.server.user.dto;

import eu.bbmri_eric.quality.server.user.domain.User;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

  private final UserDTO user;
  private final String password;

  public CustomUserDetails(User user) {
    this.user = new UserDTO(user.getUsername(), user.getId());
    this.password = user.getPassword();
    this.user.setRoles(user.getRoles());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return user.getRoles().stream()
        .map(userRole -> new SimpleGrantedAuthority(userRole.getAuthority()))
        .toList();
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
