package eu.bbmri_eric.quality.agent.user;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestUserSeedConfig {

  @Bean
  ApplicationRunner setAdminPassword(
      UserRepository userRepository, PasswordEncoder passwordEncoder) {
    return args ->
        userRepository
            .findByUsername("admin")
            .ifPresent(
                user -> {
                  user.setPassword(passwordEncoder.encode("pass"));
                  userRepository.save(user);
                });
  }
}
