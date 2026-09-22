package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditActorIdResolver;
import eu.bbmri_eric.quality.agent.audit.Audited;
import eu.bbmri_eric.quality.agent.common.CurrentUser;
import java.lang.reflect.Parameter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Records an audit log entry for every method annotated with {@link Audited}, once it returns
 * successfully, so the annotated service itself does not need to depend on {@code AuditRecorder}.
 */
@Aspect
@Component
class AuditAspect {

  private final AuditRecorder auditRecorder;
  private final ObjectProvider<AuditActorIdResolver> actorIdResolver;
  private final ExpressionParser expressionParser = new SpelExpressionParser();

  AuditAspect(AuditRecorder auditRecorder, ObjectProvider<AuditActorIdResolver> actorIdResolver) {
    this.auditRecorder = auditRecorder;
    this.actorIdResolver = actorIdResolver;
  }

  @AfterReturning(value = "@annotation(audited)", returning = "result")
  void recordAuditedMethod(JoinPoint joinPoint, Audited audited, Object result) {
    Long entityId = resolveEntityId(audited.entityId(), joinPoint, result);
    String module = audited.module().isBlank() ? null : audited.module();
    String details = audited.details().isBlank() ? null : audited.details();
    Authentication authentication = CurrentUser.authentication();
    String actor = authentication == null ? null : authentication.getName();
    auditRecorder.record(
        AuditRecord.of(audited.action())
            .actor(actor, resolveActorId(authentication))
            .module(module)
            .entityId(entityId)
            .details(details)
            .build());
  }

  private Long resolveActorId(Authentication authentication) {
    if (authentication == null) {
      return null;
    }
    AuditActorIdResolver resolver = actorIdResolver.getIfAvailable();
    return resolver == null ? null : resolver.resolveActorId(authentication);
  }

  private Long resolveEntityId(String expression, JoinPoint joinPoint, Object result) {
    if (expression.isBlank()) {
      return null;
    }
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("result", result);
    Parameter[] parameters =
        ((MethodSignature) joinPoint.getSignature()).getMethod().getParameters();
    Object[] args = joinPoint.getArgs();
    for (int i = 0; i < parameters.length; i++) {
      context.setVariable(parameters[i].getName(), args[i]);
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
