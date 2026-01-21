package lcl.afx.logging.aspect;

import lcl.afx.logging.annotation.LogDatabase;
import lcl.afx.logging.util.LogHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aspect pour l'étape 4: Accès à la base de données.
 */
@Aspect
public class LogDatabaseAspect {

    @Around("@annotation(lcl.afx.logging.annotation.LogDatabase)")
    public Object logDatabase(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogDatabase annotation = method.getAnnotation(LogDatabase.class);

        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        LogHelper.DatabaseLogHelper helper = LogHelper.database(log);

        String operation = annotation.value().isEmpty() 
            ? method.getName().toUpperCase() 
            : annotation.value();

        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        long startTime = System.nanoTime();

        try {
            // [DB] operation params={...}
            if (annotation.logParams() && args != null && args.length > 0) {
                Map<String, Object> params = new LinkedHashMap<>();
                for (int i = 0; i < args.length && i < paramNames.length; i++) {
                    params.put(paramNames[i], args[i]);
                }
                helper.start(operation, params);
            } else {
                helper.start(operation);
            }

            // Exécution
            Object result = joinPoint.proceed();

            long timeMs = (System.nanoTime() - startTime) / 1_000_000;

            // [DB] operation Success
            if (annotation.logResult()) {
                if (result instanceof Collection) {
                    helper.success(operation, timeMs, ((Collection<?>) result).size());
                } else if (result instanceof Integer || result instanceof Long) {
                    helper.success(operation, timeMs, ((Number) result).intValue());
                } else {
                    helper.success(operation, timeMs, result);
                }
            }

            return result;

        } catch (Exception e) {
            long timeMs = (System.nanoTime() - startTime) / 1_000_000;
            helper.failed(operation, timeMs, e.getMessage());
            throw e;
        }
    }
}
