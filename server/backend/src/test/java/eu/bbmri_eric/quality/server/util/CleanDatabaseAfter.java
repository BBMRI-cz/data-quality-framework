package eu.bbmri_eric.quality.server.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation to clean the database and rerun migrations after a specific test method completes.
 *
 * <p>This annotation uses Flyway to clean all database objects and rerun migrations, ensuring a
 * fresh database state for subsequent tests.
 *
 * <p>Use this annotation on individual test methods that need complete database isolation:
 *
 * <pre>
 * &#64;Test
 * &#64;CleanDatabase
 * void myTest() {
 *   // test that needs clean database afterwards
 * }
 * </pre>
 *
 * <p>Note: This is more expensive than {@code @Transactional} rollback but provides complete
 * isolation including sequence resets and constraint checks.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CleanDatabaseExtension.class)
public @interface CleanDatabaseAfter {}
