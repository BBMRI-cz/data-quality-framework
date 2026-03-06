package eu.bbmri_eric.quality.server;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class TestServerApplication {

  public static void main(String[] args) {
    SpringApplication.from(ServerApplication::main).with(DevContainersConfig.class).run(args);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class DevContainersConfig {
    private static final Logger log = LoggerFactory.getLogger(DevContainersConfig.class);

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
      var container =
          new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
              .withDatabaseName("quality_server")
              .withUsername("quality")
              .withPassword("quality")
              .withCreateContainerCmdModifier(
                  cmd ->
                      cmd.withHostConfig(
                          Objects.requireNonNull(cmd.getHostConfig())
                              .withPortBindings(
                                  new PortBinding(
                                      Ports.Binding.bindPort(5432), new ExposedPort(5432)))));
      log.info(
          "PostgreSQL dev container configured on localhost:5432 (db=quality_server, user=quality)");
      return container;
    }
  }
}
