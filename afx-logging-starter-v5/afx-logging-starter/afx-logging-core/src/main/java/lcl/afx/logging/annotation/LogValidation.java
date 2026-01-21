package lcl.afx.logging.annotation;

import java.lang.annotation.*;

/**
 * Étape 3: Validation de la requête.
 * 
 * Génère:
 * <ul>
 *   <li>"Validation {name} Start"</li>
 *   <li>"Validation {name} Success" ou "Validation {name} Failed: {raison}"</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogValidation {

    /** Nom de la validation */
    String value() default "";
}
