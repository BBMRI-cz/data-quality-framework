package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.Audited;
import eu.bbmri_eric.quality.agent.common.CurrentUser;
import java.lang.reflect.Method;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

  @Mock private AuditRecorder auditRecorder;
  @Mock private CurrentUser currentUser;

  private AuditAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new AuditAspect(auditRecorder, currentUser, new ObjectMapper());
  }

  @Test
  void audit_withAuthenticatedUser_recordsActorAndActorId() throws Throwable {
    when(currentUser.getUsername()).thenReturn(Optional.of("admin"));
    when(currentUser.getUserId()).thenReturn(Optional.of(7L));
    Method method = TestTarget.class.getDeclaredMethod("withParamEntityId", long.class);
    ProceedingJoinPoint joinPoint = joinPointFor(method, null, 42L);

    aspect.audit(joinPoint, method.getAnnotation(Audited.class));

    AuditRecord recorded = captureRecord();
    assertThat(recorded.action).isEqualTo(AuditAction.QUALITY_CHECK_CREATED);
    assertThat(recorded.actor).isEqualTo("admin");
    assertThat(recorded.actorId).isEqualTo(7L);
    assertThat(recorded.details).isNull();
    assertThat(recorded.module).isEqualTo("dataquality");
    assertThat(recorded.entityId).isEqualTo(42L);
  }

  @Test
  void audit_withoutUser_recordsNullActorAndActorId() throws Throwable {
    when(currentUser.getUsername()).thenReturn(Optional.empty());
    when(currentUser.getUserId()).thenReturn(Optional.empty());
    Method method = TestTarget.class.getDeclaredMethod("withoutEntityId");

    aspect.audit(mock(ProceedingJoinPoint.class), method.getAnnotation(Audited.class));

    AuditRecord recorded = captureRecord();
    assertThat(recorded.action).isEqualTo(AuditAction.QUALITY_CHECK_DELETED);
    assertThat(recorded.actor).isNull();
    assertThat(recorded.actorId).isNull();
    assertThat(recorded.entityId).isNull();
    assertThat(recorded.details).isNull();
  }

  @Test
  void audit_withDetails_recordsDetails() throws Throwable {
    Method method = TestTarget.class.getDeclaredMethod("withDetails");

    aspect.audit(mock(ProceedingJoinPoint.class), method.getAnnotation(Audited.class));

    assertThat(captureRecord().details).isEqualTo("setting changed to enabled");
  }

  @Test
  void audit_withEntityIdFromResult_resolvesFromReturnValue() throws Throwable {
    Method method = TestTarget.class.getDeclaredMethod("withResultEntityId");
    ProceedingJoinPoint joinPoint = joinPointFor(method, null);
    when(joinPoint.proceed()).thenReturn(99L);

    Object result = aspect.audit(joinPoint, method.getAnnotation(Audited.class));

    assertThat(result).isEqualTo(99L);
    AuditRecord recorded = captureRecord();
    assertThat(recorded.action).isEqualTo(AuditAction.REPORT_CREATED);
    assertThat(recorded.entityId).isEqualTo(99L);
  }

  @Test
  void audit_withInvalidExpression_recordsNullEntityId() throws Throwable {
    Method method = TestTarget.class.getDeclaredMethod("withInvalidExpression");

    aspect.audit(joinPointFor(method, null), method.getAnnotation(Audited.class));

    AuditRecord recorded = captureRecord();
    assertThat(recorded.action).isEqualTo(AuditAction.OTHER);
    assertThat(recorded.entityId).isNull();
  }

  @Test
  void audit_withNonNumericExpressionResult_recordsNullEntityId() throws Throwable {
    Method method = TestTarget.class.getDeclaredMethod("withNonNumericEntityId", String.class);

    aspect.audit(joinPointFor(method, null, "not-a-number"), method.getAnnotation(Audited.class));

    AuditRecord recorded = captureRecord();
    assertThat(recorded.action).isEqualTo(AuditAction.OTHER);
    assertThat(recorded.entityId).isNull();
  }

  @Test
  void audit_whenMethodThrows_doesNotRecord() throws Throwable {
    Method method = TestTarget.class.getDeclaredMethod("withoutEntityId");
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("invalid"));

    assertThatThrownBy(() -> aspect.audit(joinPoint, method.getAnnotation(Audited.class)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void audit_withDiff_appendsChangesToDetailsAndHidesSensitiveValues() throws Throwable {
    Method method = TestTarget.class.getDeclaredMethod("withDiff");
    State state = new State("old", 1, "old-secret");
    ProceedingJoinPoint joinPoint = joinPointFor(method, new StateHolder(state));
    when(joinPoint.proceed())
        .thenAnswer(
            invocation -> {
              state.name = "new";
              state.secret = "new-secret";
              return null;
            });

    aspect.audit(joinPoint, method.getAnnotation(Audited.class));

    assertThat(captureRecord().details)
        .isEqualTo("State updated: name from old to new; secret changed");
  }

  @Test
  void audit_withDiffWithoutChanges_doesNotRecord() throws Throwable {
    Method method = TestTarget.class.getDeclaredMethod("withDiff");
    ProceedingJoinPoint joinPoint =
        joinPointFor(method, new StateHolder(new State("same", 1, "secret")));

    aspect.audit(joinPoint, method.getAnnotation(Audited.class));

    verify(auditRecorder, never()).record(any());
  }

  @Test
  void audit_withInvalidDiffExpression_recordsDetailsOnly() throws Throwable {
    Method method = TestTarget.class.getDeclaredMethod("withInvalidDiff");

    aspect.audit(joinPointFor(method, new StateHolder(null)), method.getAnnotation(Audited.class));

    assertThat(captureRecord().details).isEqualTo("State updated");
  }

  private AuditRecord captureRecord() {
    ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
    verify(auditRecorder).record(captor.capture());
    return captor.getValue();
  }

  private static ProceedingJoinPoint joinPointFor(Method method, Object target, Object... args) {
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(method);
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(joinPoint.getArgs()).thenReturn(args);
    lenient().when(joinPoint.getTarget()).thenReturn(target);
    return joinPoint;
  }

  static class State {
    public String name;
    public int version;
    public String secret;

    State(String name, int version, String secret) {
      this.name = name;
      this.version = version;
      this.secret = secret;
    }
  }

  record StateHolder(State state) {
    public State current() {
      return state;
    }
  }

  private interface TestTarget {

    @Audited(action = AuditAction.QUALITY_CHECK_CREATED, module = "dataquality", entityId = "#id")
    void withParamEntityId(long id);

    @Audited(action = AuditAction.QUALITY_CHECK_DELETED)
    void withoutEntityId();

    @Audited(action = AuditAction.SETTINGS_UPDATED, details = "setting changed to enabled")
    void withDetails();

    @Audited(action = AuditAction.REPORT_CREATED, entityId = "#result")
    void withResultEntityId();

    @Audited(action = AuditAction.OTHER, entityId = "#missing.field")
    void withInvalidExpression();

    @Audited(action = AuditAction.OTHER, entityId = "#id")
    void withNonNumericEntityId(String id);

    @Audited(
        action = AuditAction.OTHER,
        details = "State updated",
        diff = "#target.current()",
        sensitive = "secret")
    void withDiff();

    @Audited(action = AuditAction.OTHER, details = "State updated", diff = "#target.missing()")
    void withInvalidDiff();
  }
}
