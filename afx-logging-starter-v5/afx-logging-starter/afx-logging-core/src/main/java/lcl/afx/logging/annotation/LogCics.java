package lcl.afx.logging.annotation;

import java.lang.annotation.*;

/**
 * Étape 5: Appel Transaction CICS.
 * 
 * Génère 4 logs:
 * <ul>
 *   <li>"[KEXX] Context: correlationId=..., userId=..."</li>
 *   <li>"[KEXX] input: KEXXCommarea.Input {...}"</li>
 *   <li>"[KEXX] output: KEXXCommarea.Output {...}"</li>
 *   <li>"[KEXX] InfosImportantes: codeRetour=..., statut=..., reference=..."</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogCics {

    /** Nom de la transaction CICS (ex: KEXX, VIRT, SOLDE) */
    String value();

    /** Champs à extraire pour InfosImportantes */
    String[] importantFields() default {"codeRetour", "statut", "reference"};
}
