package eu.bbmri_eric.quality.server.common;

import java.sql.SQLException;
import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Configuration for H2 Server used in dev mode */
@Configuration
@Profile("dev")
public class H2ServerConfig {

  @Bean(initMethod = "start", destroyMethod = "stop")
  public Server h2Server() throws SQLException {
    return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
  }
}
