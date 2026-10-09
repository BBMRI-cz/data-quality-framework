package eu.bbmri_eric.quality.server.user.impl;

import eu.bbmri_eric.quality.server.user.UserRole;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts request parameter strings to {@link UserRole} case-insensitively, so filter parameters
 * such as {@code ?roles=human_user,admin} bind correctly.
 */
@Component
class StringToUserRoleConverter implements Converter<String, UserRole> {

  @Override
  public UserRole convert(String source) {
    return UserRole.valueOf(source.trim().toUpperCase(Locale.ROOT));
  }
}
