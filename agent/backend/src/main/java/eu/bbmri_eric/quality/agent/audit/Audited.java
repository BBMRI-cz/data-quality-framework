package eu.bbmri_eric.quality.agent.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose successful completion should be recorded as an audit log entry,
 * without the method itself depending on the audit module.
 *
 * <p>An aspect ({@code AuditAspect}) intercepts every call to a method carrying this annotation and
 * records it after the method returns. The current authenticated user (if any) is used as the
 * actor. Nothing is recorded if the method throws.
 *
 * <p>With {@link #diff()}, the entry also describes what the method changed, e.g. {@code Settings
 * updated: epsilon from 1.0 to 0.5; fhirPassword changed}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

  /** The action being recorded. */
  AuditAction action();

  /** The module the action belongs to, or {@code ""} for none. */
  String module() default "";

  /**
   * A SpEL expression resolving the affected entity's ID, evaluated against the method's parameters
   * (by name, e.g. {@code #id}) and its return value (as {@code #result}). Empty means no entity ID
   * is recorded.
   */
  String entityId() default "";

  /** Details of general interest about the action, e.g. the new value of a changed property. */
  String details() default "";

  /**
   * A SpEL expression resolving the state the method changes, evaluated once before and once after
   * the method runs. The two snapshots are compared property by property (as serialized to JSON)
   * and the changed properties are appended to {@link #details()}. If nothing changed, no entry is
   * recorded. Besides the method's parameters, the expression can use the annotated bean as {@code
   * #target}, e.g. {@code #target.getSettings()} or {@code #target.findById(#id)}. Empty means no
   * diff is recorded.
   */
  String diff() default "";

  /**
   * Properties of the {@link #diff()} snapshot whose values must not be recorded; only the fact
   * that they changed is. Properties whose name contains {@code password} are always treated as
   * sensitive.
   */
  String[] sensitive() default {};
}
