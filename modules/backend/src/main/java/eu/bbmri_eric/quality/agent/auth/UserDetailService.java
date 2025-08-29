package eu.bbmri_eric.quality.agent.auth;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailService implements UserDetailsService {

  private final UserRepository users;

  public UserDetailService(UserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var u =
        users
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    var role = u.getRole();
    var authority =
        (role == null || role.isBlank())
            ? "ROLE_USER"
            : (role.startsWith("ROLE_") ? role : "ROLE_" + role);
    var authorities = List.of(new SimpleGrantedAuthority(authority));

    return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
        .password(u.getPassword())
        .authorities(authorities)
        .build();
  }
}
