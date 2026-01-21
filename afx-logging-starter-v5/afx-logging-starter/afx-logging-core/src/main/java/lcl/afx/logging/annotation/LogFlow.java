package lcl.afx.logging.annotation;

import java.lang.annotation.*;

/**
 * Active l'enchaînement logique complet des logs sur un contrôleur ou une méthode.
 * 
 * Génère automatiquement:
 * <ul>
 *   <li>1. "POST /url ControllerName Start"</li>
 *   <li>2. "Request: {payload}"</li>
 *   <li>7. "Response: {payload}"</li>
 *   <li>8. "POST /url ControllerName End"</li>
 *   <li>9. "END Request: {payload}"</li>
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogFlow {

    /** Logger la requête (étape 2) */
    boolean logRequest() default true;

    /** Logger la réponse (étape 7) */
    boolean logResponse() default true;

    /** Logger END Request (étape 9) */
    boolean logEndRequest() default true;
}
