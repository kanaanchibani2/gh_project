package lcl.afx.logging.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lcl.afx.logging.annotation.PaymentLog;
import lcl.afx.logging.masking.DataMasker;
import lcl.afx.logging.mdc.MdcKeys;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aspect AOP qui intercepte les méthodes annotées avec @PaymentLog
 * pour générer automatiquement les logs d'entrée, sortie, erreur et audit.
 */
@Aspect
public class PaymentLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(PaymentLoggingAspect.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final DataMasker dataMasker;
    private final ObjectMapper objectMapper;
    private boolean enabled = true;
    private long defaultPerformanceThresholdMs = 1000L;

    public PaymentLoggingAspect(DataMasker dataMasker) {
        this.dataMasker = dataMasker;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POINTCUTS : Définissent OÙ intercepter
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Méthodes annotées @PaymentLog
     */
    @Pointcut("@annotation(lcl.afx.logging.annotation.PaymentLog)")
    public void paymentLogMethod() {}

    /**
     * Méthodes publiques des classes annotées @PaymentLog
     */
    @Pointcut("@within(lcl.afx.logging.annotation.PaymentLog) && execution(public * *(..))")
    public void paymentLogClass() {}

    /**
     * Méthodes annotées @NoLogging (à exclure)
     */
    @Pointcut("@annotation(lcl.afx.logging.annotation.NoLogging)")
    public void noLogging() {}

    /**
     * Combinaison : (méthode OU classe) ET PAS @NoLogging
     */
    @Pointcut("(paymentLogMethod() || paymentLogClass()) && !noLogging()")
    public void loggableMethods() {}

    // ══════════════════════════════════════════════════════════════════════════
    // ADVICE : Définit QUOI faire
    // ══════════════════════════════════════════════════════════════════════════

    @Around("loggableMethods()")
    public Object logPaymentOperation(ProceedingJoinPoint joinPoint) throws Throwable {

        if (!enabled) {
            return joinPoint.proceed();
        }

        // ──────────────────────────────────────────────────────────────────────
        // 1. EXTRAIRE LES MÉTADONNÉES
        // ──────────────────────────────────────────────────────────────────────
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        PaymentLog annotation = getAnnotation(method, targetClass);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        String operationId = UUID.randomUUID().toString().substring(0, 8);
        String operation = annotation.operation().isBlank()
            ? method.getName().toUpperCase()
            : annotation.operation();

        // ──────────────────────────────────────────────────────────────────────
        // 2. ENRICHIR LE MDC
        // ──────────────────────────────────────────────────────────────────────
        MDC.put(MdcKeys.OPERATION, operation);
        MDC.put(MdcKeys.OPERATION_ID, operationId);

        long startTime = System.nanoTime();

        try {
            // ──────────────────────────────────────────────────────────────────
            // 3. LOG ENTRY
            // ──────────────────────────────────────────────────────────────────
            logEntry(annotation, operation, joinPoint.getArgs(), signature.getParameterNames());

            // ──────────────────────────────────────────────────────────────────
            // 4. EXÉCUTER LA VRAIE MÉTHODE
            // ──────────────────────────────────────────────────────────────────
            Object result = joinPoint.proceed();

            // ──────────────────────────────────────────────────────────────────
            // 5. LOG EXIT
            // ──────────────────────────────────────────────────────────────────
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            logExit(annotation, operation, result, executionTimeMs);

            // ──────────────────────────────────────────────────────────────────
            // 6. VÉRIFIER PERFORMANCE
            // ──────────────────────────────────────────────────────────────────
            checkPerformance(annotation, operation, executionTimeMs);

            // ──────────────────────────────────────────────────────────────────
            // 7. AUDIT TRAIL (si activé)
            // ──────────────────────────────────────────────────────────────────
            if (annotation.auditEnabled()) {
                logAudit(operation, operationId, "SUCCESS", executionTimeMs, null);
            }

            return result;

        } catch (Exception e) {
            // ──────────────────────────────────────────────────────────────────
            // 8. LOG ERROR
            // ──────────────────────────────────────────────────────────────────
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            logError(operation, e, executionTimeMs);

            if (annotation.auditEnabled()) {
                logAudit(operation, operationId, "FAILURE", executionTimeMs, e);
            }

            throw e;

        } finally {
            // ──────────────────────────────────────────────────────────────────
            // 9. NETTOYER LE MDC (TOUJOURS)
            // ──────────────────────────────────────────────────────────────────
            MDC.remove(MdcKeys.OPERATION);
            MDC.remove(MdcKeys.OPERATION_ID);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MÉTHODES PRIVÉES
    // ══════════════════════════════════════════════════════════════════════════

    private PaymentLog getAnnotation(Method method, Class<?> targetClass) {
        // Priorité à l'annotation sur la méthode
        PaymentLog annotation = method.getAnnotation(PaymentLog.class);
        if (annotation != null) {
            return annotation;
        }
        // Sinon, chercher sur la classe
        return targetClass.getAnnotation(PaymentLog.class);
    }

    private void logEntry(PaymentLog annotation, String operation,
                          Object[] args, String[] paramNames) {
        if (!isLevelEnabled(annotation.entryLevel())) {
            return;
        }

        if (annotation.logParams() && args != null && args.length > 0) {
            Map<String, Object> params = new LinkedHashMap<>();
            for (int i = 0; i < args.length; i++) {
                String name = (paramNames != null && i < paramNames.length)
                    ? paramNames[i] : "arg" + i;
                params.put(name, maskObject(args[i]));
            }
            logAtLevel(annotation.entryLevel(),
                "▶ ENTRY [{}] params={}", operation, serialize(params));
        } else {
            logAtLevel(annotation.entryLevel(), "▶ ENTRY [{}]", operation);
        }
    }

    private void logExit(PaymentLog annotation, String operation,
                         Object result, long timeMs) {
        if (!isLevelEnabled(annotation.exitLevel())) {
            return;
        }

        if (annotation.logResult() && result != null) {
            logAtLevel(annotation.exitLevel(),
                "◀ EXIT [{}] time={}ms result={}",
                operation, timeMs, maskObject(result));
        } else {
            logAtLevel(annotation.exitLevel(),
                "◀ EXIT [{}] time={}ms", operation, timeMs);
        }
    }

    private void logError(String operation, Exception e, long timeMs) {
        String maskedMessage = dataMasker.mask(e.getMessage());
        log.error("✖ ERROR [{}] time={}ms error={}", operation, timeMs, maskedMessage, e);
    }

    private void checkPerformance(PaymentLog annotation, String operation, long timeMs) {
        long threshold = annotation.performanceThresholdMs() > 0
            ? annotation.performanceThresholdMs()
            : defaultPerformanceThresholdMs;

        if (timeMs > threshold) {
            log.warn("⚠ SLOW [{}] {}ms > threshold {}ms", operation, timeMs, threshold);
        }
    }

    private void logAudit(String operation, String operationId, String status,
                          long timeMs, Exception e) {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("audit_type", "PAYMENT_OPERATION");
        audit.put("timestamp", Instant.now().toString());
        audit.put("operation", operation);
        audit.put("operation_id", operationId);
        audit.put("status", status);
        audit.put("execution_time_ms", timeMs);
        audit.put("correlation_id", MDC.get(MdcKeys.CORRELATION_ID));
        audit.put("user_id", MDC.get(MdcKeys.USER_ID));
        audit.put("client_ip", MDC.get(MdcKeys.CLIENT_IP));

        if (e != null) {
            audit.put("error_type", e.getClass().getName());
            audit.put("error_message", dataMasker.mask(e.getMessage()));
        }

        // Logger AUDIT séparé (fichier différent, rétention longue)
        auditLog.info("AUDIT: {}", serialize(audit));
    }

    private Object maskObject(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            String masked = dataMasker.mask(json);
            return objectMapper.readValue(masked, Object.class);
        } catch (JsonProcessingException e) {
            return dataMasker.mask(obj.toString());
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    private boolean isLevelEnabled(PaymentLog.LogLevel level) {
        return switch (level) {
            case TRACE -> log.isTraceEnabled();
            case DEBUG -> log.isDebugEnabled();
            case INFO -> log.isInfoEnabled();
            case WARN -> log.isWarnEnabled();
        };
    }

    private void logAtLevel(PaymentLog.LogLevel level, String format, Object... args) {
        switch (level) {
            case TRACE -> log.trace(format, args);
            case DEBUG -> log.debug(format, args);
            case INFO -> log.info(format, args);
            case WARN -> log.warn(format, args);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Setters pour configuration
    // ══════════════════════════════════════════════════════════════════════════

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDefaultPerformanceThresholdMs(long defaultPerformanceThresholdMs) {
        this.defaultPerformanceThresholdMs = defaultPerformanceThresholdMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getDefaultPerformanceThresholdMs() {
        return defaultPerformanceThresholdMs;
    }
}
