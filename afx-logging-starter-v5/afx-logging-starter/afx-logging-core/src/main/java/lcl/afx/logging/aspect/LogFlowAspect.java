package lcl.afx.logging.aspect;

import lcl.afx.logging.annotation.LogFlow;
import lcl.afx.logging.util.LogHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

/**
 * Aspect pour l'enchaînement logique des contrôleurs (étapes 1, 2, 7, 8, 9).
 */
@Aspect
public class LogFlowAspect {

    @Pointcut("@annotation(lcl.afx.logging.annotation.LogFlow)")
    public void logFlowMethod() {}

    @Pointcut("@within(lcl.afx.logging.annotation.LogFlow)")
    public void logFlowClass() {}

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {}

    @Around("(logFlowMethod() || logFlowClass()) && publicMethod()")
    public Object logFlow(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        
        Logger log = LoggerFactory.getLogger(targetClass);
        LogHelper.ControllerLogHelper helper = LogHelper.controller(log);

        String controllerName = targetClass.getSimpleName();
        String httpMethod = extractHttpMethod(method);
        String url = extractUrl(method, targetClass);

        LogFlow annotation = getAnnotation(method, targetClass);
        boolean logRequest = annotation == null || annotation.logRequest();
        boolean logResponse = annotation == null || annotation.logResponse();
        boolean logEndRequest = annotation == null || annotation.logEndRequest();

        // Extraire le body de la requête
        Object requestBody = extractRequestBody(joinPoint.getArgs(), signature.getParameterAnnotations());

        try {
            // ═══════════════════════════════════════════════════════════════════
            // ÉTAPE 1: Début du contrôleur
            // ═══════════════════════════════════════════════════════════════════
            helper.start(httpMethod, url, controllerName);

            // ═══════════════════════════════════════════════════════════════════
            // ÉTAPE 2: Log de la requête métier
            // ═══════════════════════════════════════════════════════════════════
            if (logRequest && requestBody != null) {
                helper.request(requestBody);
            }

            // ═══════════════════════════════════════════════════════════════════
            // EXÉCUTION
            // ═══════════════════════════════════════════════════════════════════
            Object result = joinPoint.proceed();

            // ═══════════════════════════════════════════════════════════════════
            // ÉTAPE 7: Réponse API client
            // ═══════════════════════════════════════════════════════════════════
            if (logResponse && result != null) {
                Object responseBody = extractResponseBody(result);
                helper.response(responseBody);
            }

            // ═══════════════════════════════════════════════════════════════════
            // ÉTAPE 8: Fin du contrôleur
            // ═══════════════════════════════════════════════════════════════════
            helper.end(httpMethod, url, controllerName);

            // ═══════════════════════════════════════════════════════════════════
            // ÉTAPE 9: Fin de la requête métier
            // ═══════════════════════════════════════════════════════════════════
            if (logEndRequest && requestBody != null) {
                helper.endRequest(requestBody);
            }

            return result;

        } catch (Exception e) {
            log.error("{} {} {} Error: {}", httpMethod, url, controllerName, 
                LogHelper.mask(e.getMessage()));
            throw e;
        }
    }

    private LogFlow getAnnotation(Method method, Class<?> targetClass) {
        LogFlow annotation = method.getAnnotation(LogFlow.class);
        return annotation != null ? annotation : targetClass.getAnnotation(LogFlow.class);
    }

    private String extractHttpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = method.getAnnotation(RequestMapping.class);
            if (rm.method().length > 0) return rm.method()[0].name();
        }
        return "HTTP";
    }

    private String extractUrl(Method method, Class<?> targetClass) {
        StringBuilder path = new StringBuilder();

        RequestMapping classMapping = targetClass.getAnnotation(RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            path.append(classMapping.value()[0]);
        }

        String methodPath = "";
        if (method.isAnnotationPresent(GetMapping.class)) {
            methodPath = getFirstValue(method.getAnnotation(GetMapping.class).value());
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            methodPath = getFirstValue(method.getAnnotation(PostMapping.class).value());
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            methodPath = getFirstValue(method.getAnnotation(PutMapping.class).value());
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            methodPath = getFirstValue(method.getAnnotation(DeleteMapping.class).value());
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            methodPath = getFirstValue(method.getAnnotation(PatchMapping.class).value());
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            methodPath = getFirstValue(method.getAnnotation(RequestMapping.class).value());
        }

        path.append(methodPath);
        return path.length() > 0 ? path.toString() : "/";
    }

    private String getFirstValue(String[] values) {
        return values != null && values.length > 0 ? values[0] : "";
    }

    private Object extractRequestBody(Object[] args, java.lang.annotation.Annotation[][] paramAnnotations) {
        if (args == null) return null;
        for (int i = 0; i < args.length; i++) {
            for (java.lang.annotation.Annotation ann : paramAnnotations[i]) {
                if (ann instanceof RequestBody) {
                    return args[i];
                }
            }
        }
        return null;
    }

    private Object extractResponseBody(Object result) {
        if (result == null) return null;
        if (result.getClass().getSimpleName().equals("ResponseEntity")) {
            try {
                Method getBody = result.getClass().getMethod("getBody");
                return getBody.invoke(result);
            } catch (Exception e) {
                return result;
            }
        }
        return result;
    }
}
