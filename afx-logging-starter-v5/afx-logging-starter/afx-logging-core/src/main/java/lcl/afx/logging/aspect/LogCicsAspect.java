package lcl.afx.logging.aspect;

import lcl.afx.logging.annotation.LogCics;
import lcl.afx.logging.util.LogHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aspect pour l'étape 5: Appel Transaction CICS.
 */
@Aspect
public class LogCicsAspect {

    @Around("@annotation(lcl.afx.logging.annotation.LogCics)")
    public Object logCics(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogCics annotation = method.getAnnotation(LogCics.class);

        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String txName = annotation.value();
        LogHelper.CicsLogHelper helper = LogHelper.cics(log, txName);

        Object[] args = joinPoint.getArgs();
        long startTime = System.nanoTime();

        try {
            // ═══════════════════════════════════════════════════════════════════
            // LOG 1: [KEXX] Context
            // ═══════════════════════════════════════════════════════════════════
            String correlationId = MDC.get("corrId");
            String userId = MDC.get("usrId");
            helper.context(correlationId, userId);

            // ═══════════════════════════════════════════════════════════════════
            // LOG 2: [KEXX] input
            // ═══════════════════════════════════════════════════════════════════
            if (args != null && args.length > 0) {
                helper.input(args[0]);
            }

            // ═══════════════════════════════════════════════════════════════════
            // EXÉCUTION
            // ═══════════════════════════════════════════════════════════════════
            Object result = joinPoint.proceed();

            long timeMs = (System.nanoTime() - startTime) / 1_000_000;

            // ═══════════════════════════════════════════════════════════════════
            // LOG 3: [KEXX] output
            // ═══════════════════════════════════════════════════════════════════
            if (result != null) {
                helper.output(result);
            }

            // ═══════════════════════════════════════════════════════════════════
            // LOG 4: [KEXX] InfosImportantes
            // ═══════════════════════════════════════════════════════════════════
            if (result != null) {
                Map<String, Object> infos = extractFields(result, annotation.importantFields());
                infos.put("executionTimeMs", timeMs);
                helper.infos(infos);
            }

            return result;

        } catch (Exception e) {
            long timeMs = (System.nanoTime() - startTime) / 1_000_000;
            helper.error(e.getMessage(), timeMs);
            throw e;
        }
    }

    private Map<String, Object> extractFields(Object obj, String[] fieldNames) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (obj == null || fieldNames == null) return result;

        Class<?> clazz = obj.getClass();
        for (String fieldName : fieldNames) {
            try {
                Field field = findField(clazz, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value != null) {
                        result.put(fieldName, value);
                    }
                } else {
                    // Try getter
                    String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                    try {
                        Method getter = clazz.getMethod(getterName);
                        Object value = getter.invoke(obj);
                        if (value != null) {
                            result.put(fieldName, value);
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
