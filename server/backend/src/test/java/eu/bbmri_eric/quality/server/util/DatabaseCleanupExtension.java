package eu.bbmri_eric.quality.server.util;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class DatabaseCleanupExtension implements AfterAllCallback {

  @Override
  public void afterAll(ExtensionContext context) {
    Flyway flyway = SpringExtension.getApplicationContext(context).getBean(Flyway.class);
    flyway.clean();
    flyway.migrate();
  }
}
