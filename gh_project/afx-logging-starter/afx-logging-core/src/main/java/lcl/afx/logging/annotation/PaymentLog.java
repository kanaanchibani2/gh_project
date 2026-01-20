package lcl.afx.logging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour activer le logging automatique sur une méthode ou une classe.
 * 
 * <p>Exemple d'utilisation:</p>
 * <pre>
 * &#64;PaymentLog(operation = "SEPA_TRANSFER", auditEnabled = true)
 * public TransferResult executeTransfer(TransferRequest request) {
 *     // ...
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PaymentLog {

    /**
     * Nom de l'opération (ex: "SEPA_TRANSFER", "CARD_PAYMENT").
     * Si vide, utilise le nom de la méthode en majuscules.
     */
    String operation() default "";

    /**
     * Logger les paramètres d'entrée (masqués automatiquement).
     */
    boolean logParams() default true;

    /**
     * Logger le résultat de retour (masqué automatiquement).
     */
    boolean logResult() default true;

    /**
     * Activer l'audit trail (fichier séparé, rétention longue).
     */
    boolean auditEnabled() default false;

    /**
     * Seuil de performance en millisecondes.
     * Une alerte WARN est générée si le temps d'exécution dépasse ce seuil.
     */
    long performanceThresholdMs() default 1000L;

    /**
     * Niveau de log pour l'entrée de méthode.
     */
    LogLevel entryLevel() default LogLevel.INFO;

    /**
     * Niveau de log pour la sortie de méthode.
     */
    LogLevel exitLevel() default LogLevel.INFO;

    /**
     * Niveaux de log supportés.
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN
    }
}
