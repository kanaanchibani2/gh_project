package lcl.afx.logging.annotation;

import java.lang.annotation.*;

/**
 * Étape 4: Accès à la base de données.
 * 
 * Génère:
 * <ul>
 *   <li>"[DB] {operation} params={...}"</li>
 *   <li>"[DB] {operation} Success (time=Xms) result={...}"</li>
 *   <li>"[DB] {operation} Failed (time=Xms): {error}"</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogDatabase {

    /** Nom de l'opération (ex: SELECT_BALANCE, UPDATE_ACCOUNT) */
    String value() default "";

    /** Logger les paramètres */
    boolean logParams() default true;

    /** Logger le résultat */
    boolean logResult() default true;
}
