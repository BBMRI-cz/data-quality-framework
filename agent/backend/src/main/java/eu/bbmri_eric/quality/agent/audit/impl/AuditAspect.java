package eu.bbmri_eric.quality.agent.audit.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.audit.Audited;
import eu.bbmri_eric.quality.agent.common.CurrentUser;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Records an audit log entry for every method annotated with {@link Audited}, once it returns
 * successfully, so the annotated service itself does not need to depend on {@code AuditRecorder}.
 */
@Aspect
@Component
class AuditAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
  private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {};

  private final AuditRecorder auditRecorder;
  private final CurrentUser currentUser;
  private final ObjectMapper objectMapper;
  private final ExpressionParser expressionParser = new SpelExpressionParser();

  AuditAspect(AuditRecorder auditRecorder, CurrentUser currentUser, ObjectMapper objectMapper) {
    this.auditRecorder = auditRecorder;
    this.currentUser = currentUser;
    this.objectMapper = objectMapper;
  }

  @Around("@annotation(audited)")
  Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
    StandardEvaluationContext context =
        audited.entityId().isBlank() && audited.diff().isBlank() ? null : createContext(joinPoint);
    Map<String, Object> before = snapshot(audited.diff(), context);

    Object result = joinPoint.proceed();

    String details = audited.details().isBlank() ? null : audited.details();
    if (before != null) {
      Map<String, Object> after = snapshot(audited.diff(), context);
      if (after != null) {
        String changes = AuditDiff.describe(before, after, Set.of(audited.sensitive()));
        if (changes.isEmpty()) {
          return result;
        }
        details = details == null ? changes : details + ": " + changes;
      }
    }
    if (context != null) {
      context.setVariable("result", result);
    }
    auditRecorder.record(
        AuditRecord.of(audited.action())
            .actor(currentUser.getUsername().orElse(null), currentUser.getUserId().orElse(null))
            .module(audited.module().isBlank() ? null : audited.module())
            .entityId(resolveEntityId(audited.entityId(), context))
            .details(details)
            .build());
    return result;
  }

  private static StandardEvaluationContext createContext(JoinPoint joinPoint) {
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("target", joinPoint.getTarget());
    Parameter[] parameters =
        ((MethodSignature) joinPoint.getSignature()).getMethod().getParameters();
    Object[] args = joinPoint.getArgs();
    for (int i = 0; i < parameters.length; i++) {
      context.setVariable(parameters[i].getName(), args[i]);
    }
    return context;
  }

  /**
   * Evaluates the diff expression and copies its value into a map right away, so later changes to
   * the returned object (e.g. a managed entity) do not affect the snapshot.
   *
   * @return the snapshot, or {@code null} if there is no expression or it cannot be evaluated
   */
  private Map<String, Object> snapshot(String expression, StandardEvaluationContext context) {
    if (expression.isBlank()) {
      return null;
    }
    try {
      Object value = expressionParser.parseExpression(expression).getValue(context);
      return objectMapper.convertValue(value, SNAPSHOT_TYPE);
    } catch (RuntimeException ex) {
      log.warn("Cannot evaluate audit diff expression '{}': {}", expression, ex.getMessage());
      return null;
    }
  }

  private Long resolveEntityId(String expression, StandardEvaluationContext context) {
    if (expression.isBlank()) {
      return null;
    }
    Object value;
    try {
      value = expressionParser.parseExpression(expression).getValue(context);
    } catch (RuntimeException ex) {
      return null;
    }
    if (!(value instanceof Number number)) {
      return null;
    }
    return number.longValue();
  }
}
