package lcl.afx.logging.annotation;

import java.lang.annotation.*;

/**
 * Étape 6: Appel REST (API) externe.
 * 
 * Génère 4 logs:
 * <ul>
 *   <li>"[API] Context: service=..., operation=..."</li>
 *   <li>"[API][POST /url] [REQUETE] {...}"</li>
 *   <li>"[API][POST /url] [REPONSE] 200 {...}"</li>
 *   <li>"[API] InfosImportantes: service=..., statut=..., tempsReponseMs=..."</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogApi {

    /** Nom du service appelé (ex: notification-service, risk-api) */
    String value();
}
