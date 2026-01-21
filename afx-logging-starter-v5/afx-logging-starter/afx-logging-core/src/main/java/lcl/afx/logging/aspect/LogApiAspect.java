package lcl.afx.logging.aspect;

import lcl.afx.logging.annotation.LogApi;
import lcl.afx.logging.util.LogHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Method;

/**
 * Aspect pour l'étape 6: Appel REST (API) externe.
 */
@Aspect
public class LogApiAspect {

    @Around("@annotation(lcl.afx.logging.annotation.LogApi)")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogApi annotation = method.getAnnotation(LogApi.class);

        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String serviceName = annotation.value();
        LogHelper.ApiLogHelper helper = LogHelper.api(log, serviceName);

        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        // Extraire URL et body
        String url = extractUrl(args, paramNames);
        String httpMethod = extractHttpMethod(method.getName());
        Object requestBody = extractBody(args, paramNames);

        long startTime = System.nanoTime();

        try {
            // ═══════════════════════════════════════════════════════════════════
            // LOG 1: [API] Context
            // ═══════════════════════════════════════════════════════════════════
            String correlationId = MDC.get("corrId");
            helper.context(method.getName(), correlationId);

            // ═══════════════════════════════════════════════════════════════════
            // LOG 2: [API][POST /url] [REQUETE] body
            // ═══════════════════════════════════════════════════════════════════
            if (requestBody != null) {
                helper.request(httpMethod, url, requestBody);
            }

            // ═══════════════════════════════════════════════════════════════════
            // EXÉCUTION
            // ═══════════════════════════════════════════════════════════════════
            Object result = joinPoint.proceed();

            long timeMs = (System.nanoTime() - startTime) / 1_000_000;
            int status = extractStatus(result);

            // ═══════════════════════════════════════════════════════════════════
            // LOG 3: [API][POST /url] [REPONSE] statut + body
            // ═══════════════════════════════════════════════════════════════════
            if (result != null) {
                helper.response(httpMethod, url, status, extractResponseBody(result));
            }

            // ═══════════════════════════════════════════════════════════════════
            // LOG 4: [API] InfosImportantes
            // ═══════════════════════════════════════════════════════════════════
            helper.infos(status, timeMs);

            return result;

        } catch (Exception e) {
            long timeMs = (System.nanoTime() - startTime) / 1_000_000;
            helper.error(httpMethod, url, e.getMessage());
            helper.infos(500, timeMs, e.getMessage());
            throw e;
        }
    }

    private String extractUrl(Object[] args, String[] paramNames) {
        if (args == null || paramNames == null) return "/";
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            String name = paramNames[i].toLowerCase();
            if (name.contains("url") || name.contains("uri") || name.contains("path")) {
                return args[i] != null ? args[i].toString() : "/";
            }
        }
        // Chercher une String qui ressemble à une URL
        for (Object arg : args) {
            if (arg instanceof String) {
                String s = (String) arg;
                if (s.startsWith("/") || s.startsWith("http")) {
                    return s;
                }
            }
        }
        return "/";
    }

    private String extractHttpMethod(String methodName) {
        String lower = methodName.toLowerCase();
        if (lower.contains("get") || lower.contains("fetch") || lower.contains("find")) return "GET";
        if (lower.contains("post") || lower.contains("create") || lower.contains("send")) return "POST";
        if (lower.contains("put") || lower.contains("update")) return "PUT";
        if (lower.contains("delete") || lower.contains("remove")) return "DELETE";
        return "POST";
    }

    private Object extractBody(Object[] args, String[] paramNames) {
        if (args == null || args.length == 0) return null;
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            String name = paramNames[i].toLowerCase();
            if (name.contains("body") || name.contains("request") || name.contains("payload") || name.contains("data")) {
                return args[i];
            }
        }
        // Prendre le premier objet non-URL
        for (Object arg : args) {
            if (arg != null && !(arg instanceof String)) {
                return arg;
            }
        }
        return null;
    }

    private int extractStatus(Object result) {
        if (result == null) return 200;
        if (result.getClass().getSimpleName().equals("ResponseEntity")) {
            try {
                Method getStatusCode = result.getClass().getMethod("getStatusCodeValue");
                return (int) getStatusCode.invoke(result);
            } catch (Exception e) {
                return 200;
            }
        }
        return 200;
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
