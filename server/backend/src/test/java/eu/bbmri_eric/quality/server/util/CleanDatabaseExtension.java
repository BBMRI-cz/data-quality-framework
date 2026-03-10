package eu.bbmri_eric.quality.server.util;

import java.lang.reflect.Method;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class CleanDatabaseExtension implements AfterEachCallback {

  @Override
  public void afterEach(ExtensionContext context) {
    // Check if the test method has @CleanDatabase annotation
    Method testMethod = context.getRequiredTestMethod();
    if (!testMethod.isAnnotationPresent(CleanDatabaseAfter.class)) {
      return;
    }

    Flyway flyway = SpringExtension.getApplicationContext(context).getBean(Flyway.class);

    // Clean the database (drops all objects)
    flyway.clean();

    // Rerun migrations to restore schema
    flyway.migrate();
  }
}
