package lcl.afx.logging.aspect;

import lcl.afx.logging.annotation.LogValidation;
import lcl.afx.logging.util.LogHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Aspect pour l'étape 3: Validation de la requête.
 */
@Aspect
public class LogValidationAspect {

    @Around("@annotation(lcl.afx.logging.annotation.LogValidation)")
    public Object logValidation(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogValidation annotation = method.getAnnotation(LogValidation.class);

        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        LogHelper.ValidationLogHelper helper = LogHelper.validation(log);

        String validationName = annotation.value().isEmpty() 
            ? method.getName() 
            : annotation.value();

        try {
            // Validation Start
            helper.start(validationName);

            // Exécution
            Object result = joinPoint.proceed();

            // Validation Success
            helper.success(validationName);

            return result;

        } catch (Exception e) {
            // Validation Failed
            helper.failed(validationName, e.getMessage());
            throw e;
        }
    }
}
