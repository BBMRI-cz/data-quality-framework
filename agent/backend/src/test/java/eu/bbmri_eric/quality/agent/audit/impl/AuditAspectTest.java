package eu.bbmri_eric.quality.agent.audit.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.AuditActorIdResolver;
import eu.bbmri_eric.quality.agent.audit.Audited;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

  @Mock private AuditRecorder auditRecorder;
  @Mock private ObjectProvider<AuditActorIdResolver> actorIdResolverProvider;
  @Mock private AuditActorIdResolver actorIdResolver;

  private AuditAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new AuditAspect(auditRecorder, actorIdResolverProvider);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void recordAuditedMethod_withAuthenticatedUser_recordsActorAndResolvedActorId() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("admin", null, "ROLE_ADMIN"));
    when(actorIdResolverProvider.getIfAvailable()).thenReturn(actorIdResolver);
    when(actorIdResolver.resolveActorId(any(Authentication.class))).thenReturn(7L);
    Method method = TestTarget.class.getDeclaredMethod("withParamEntityId", long.class);
    Audited audited = method.getAnnotation(Audited.class);
    JoinPoint joinPoint = joinPointFor(method, 42L);

    aspect.recordAuditedMethod(joinPoint, audited, null);

    verify(auditRecorder)
        .record(
            eq(AuditAction.QUALITY_CHECK_CREATED),
            eq("admin"),
            eq(7L),
            isNull(),
            eq("dataquality"),
            eq(42L));
  }

  @Test
  void recordAuditedMethod_withoutAuthentication_recordsNullActorAndDoesNotResolveActorId()
      throws Exception {
    Method method = TestTarget.class.getDeclaredMethod("withoutEntityId");
    Audited audited = method.getAnnotation(Audited.class);
    JoinPoint joinPoint = mock(JoinPoint.class);

    aspect.recordAuditedMethod(joinPoint, audited, null);

    verify(auditRecorder)
        .record(
            eq(AuditAction.QUALITY_CHECK_DELETED),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull());
    verifyNoInteractions(actorIdResolverProvider);
  }

  @Test
  void recordAuditedMethod_authenticatedWithNoResolverBean_recordsNullActorId() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("admin", null, "ROLE_ADMIN"));
    when(actorIdResolverProvider.getIfAvailable()).thenReturn(null);
    Method method = TestTarget.class.getDeclaredMethod("withoutEntityId");
    Audited audited = method.getAnnotation(Audited.class);
    JoinPoint joinPoint = mock(JoinPoint.class);

    aspect.recordAuditedMethod(joinPoint, audited, null);

    verify(auditRecorder)
        .record(
            eq(AuditAction.QUALITY_CHECK_DELETED),
            eq("admin"),
            isNull(),
            isNull(),
            isNull(),
            isNull());
  }

  @Test
  void recordAuditedMethod_withEntityIdFromResult_resolvesFromReturnValue() throws Exception {
    Method method = TestTarget.class.getDeclaredMethod("withResultEntityId");
    Audited audited = method.getAnnotation(Audited.class);
    JoinPoint joinPoint = joinPointFor(method);

    aspect.recordAuditedMethod(joinPoint, audited, 99L);

    verify(auditRecorder)
        .record(eq(AuditAction.REPORT_CREATED), isNull(), isNull(), isNull(), isNull(), eq(99L));
  }

  @Test
  void recordAuditedMethod_withInvalidExpression_recordsNullEntityId() throws Exception {
    Method method = TestTarget.class.getDeclaredMethod("withInvalidExpression");
    Audited audited = method.getAnnotation(Audited.class);
    JoinPoint joinPoint = joinPointFor(method);

    aspect.recordAuditedMethod(joinPoint, audited, null);

    verify(auditRecorder)
        .record(eq(AuditAction.OTHER), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  void recordAuditedMethod_withNonNumericExpressionResult_recordsNullEntityId() throws Exception {
    Method method = TestTarget.class.getDeclaredMethod("withNonNumericEntityId", String.class);
    Audited audited = method.getAnnotation(Audited.class);
    JoinPoint joinPoint = joinPointFor(method, "not-a-number");

    aspect.recordAuditedMethod(joinPoint, audited, null);

    verify(auditRecorder)
        .record(eq(AuditAction.OTHER), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  private static JoinPoint joinPointFor(Method method, Object... args) {
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(method);
    JoinPoint joinPoint = mock(JoinPoint.class);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(joinPoint.getArgs()).thenReturn(args);
    return joinPoint;
  }

  private interface TestTarget {

    @Audited(action = AuditAction.QUALITY_CHECK_CREATED, module = "dataquality", entityId = "#id")
    void withParamEntityId(long id);

    @Audited(action = AuditAction.QUALITY_CHECK_DELETED)
    void withoutEntityId();

    @Audited(action = AuditAction.REPORT_CREATED, entityId = "#result")
    void withResultEntityId();

    @Audited(action = AuditAction.OTHER, entityId = "#missing.field")
    void withInvalidExpression();

    @Audited(action = AuditAction.OTHER, entityId = "#id")
    void withNonNumericEntityId(String id);
  }
}
