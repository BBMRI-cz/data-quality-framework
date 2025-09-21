package eu.bbmri_eric.quality.agent.user;

import static org.springframework.security.core.userdetails.User.withUsername;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class UserDetailService implements UserDetailsService {

  private final UserRepository userRepository;

  UserDetailService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return withUsername(user.getUsername()).password(user.getPassword()).build();
  }
}
