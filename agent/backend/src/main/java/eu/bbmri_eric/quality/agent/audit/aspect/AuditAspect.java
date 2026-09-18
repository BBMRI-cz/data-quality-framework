package eu.bbmri_eric.quality.agent.audit.aspect;

import eu.bbmri_eric.quality.agent.audit.AuditService;
import eu.bbmri_eric.quality.agent.audit.Audited;
import eu.bbmri_eric.quality.agent.audit.CurrentActor;
import java.lang.reflect.Parameter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Records an audit log entry for every method annotated with {@link Audited}, once it returns
 * successfully, so the annotated service itself does not need to depend on {@link AuditService}.
 */
@Aspect
@Component
class AuditAspect {

  private final AuditService auditService;
  private final ExpressionParser expressionParser = new SpelExpressionParser();

  AuditAspect(AuditService auditService) {
    this.auditService = auditService;
  }

  @AfterReturning(value = "@annotation(audited)", returning = "result")
  void recordAuditedMethod(JoinPoint joinPoint, Audited audited, Object result) {
    Long entityId = resolveEntityId(audited.entityId(), joinPoint, result);
    String module = audited.module().isBlank() ? null : audited.module();
    auditService.record(audited.action(), CurrentActor.username(), null, module, entityId);
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
    Object value = expressionParser.parseExpression(expression).getValue(context);
    return value == null ? null : ((Number) value).longValue();
  }
}
