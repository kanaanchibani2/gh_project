package lcl.afx.logging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour exclure une méthode du logging automatique.
 * 
 * <p>Utile pour les méthodes internes appelées fréquemment.</p>
 * 
 * <pre>
 * &#64;NoLogging(reason = "Méthode de validation appelée fréquemment")
 * private boolean validateIban(String iban) {
 *     return iban != null && iban.length() > 15;
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoLogging {

    /**
     * Raison de l'exclusion (documentation).
     */
    String reason() default "";
}
