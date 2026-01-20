package lcl.afx.logging.propagation;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lcl.afx.logging.mdc.MdcKeys;
import org.slf4j.MDC;

/**
 * Intercepteur Feign qui propage le correlation ID et autres
 * informations MDC dans les appels HTTP sortants.
 * 
 * <p>Ce bean est automatiquement détecté par Feign si présent dans le contexte Spring.</p>
 */
public class FeignCorrelationInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Lire du MDC et ajouter aux headers sortants
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null) {
            template.header(MdcKeys.HEADER_CORRELATION_ID, correlationId);
        }

        String transactionId = MDC.get(MdcKeys.TRANSACTION_ID);
        if (transactionId != null) {
            template.header(MdcKeys.HEADER_TRANSACTION_ID, transactionId);
        }

        String userId = MDC.get(MdcKeys.USER_ID);
        if (userId != null) {
            template.header(MdcKeys.HEADER_USER_ID, userId);
        }
    }
}
