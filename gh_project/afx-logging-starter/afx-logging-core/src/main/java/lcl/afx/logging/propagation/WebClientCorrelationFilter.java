package lcl.afx.logging.propagation;

import lcl.afx.logging.mdc.MdcKeys;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Filtre WebClient (reactive) qui propage le correlation ID et autres
 * informations MDC dans les appels HTTP sortants.
 * 
 * <p>Utilisation:</p>
 * <pre>
 * &#64;Bean
 * public WebClient webClient(WebClientCorrelationFilter filter) {
 *     return WebClient.builder()
 *         .filter(filter)
 *         .build();
 * }
 * </pre>
 */
public class WebClientCorrelationFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // Capturer le MDC du thread appelant (important en contexte réactif)
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        ClientRequest.Builder builder = ClientRequest.from(request);

        if (mdcContext != null) {
            String correlationId = mdcContext.get(MdcKeys.CORRELATION_ID);
            if (correlationId != null) {
                builder.header(MdcKeys.HEADER_CORRELATION_ID, correlationId);
            }

            String transactionId = mdcContext.get(MdcKeys.TRANSACTION_ID);
            if (transactionId != null) {
                builder.header(MdcKeys.HEADER_TRANSACTION_ID, transactionId);
            }

            String userId = mdcContext.get(MdcKeys.USER_ID);
            if (userId != null) {
                builder.header(MdcKeys.HEADER_USER_ID, userId);
            }
        }

        return next.exchange(builder.build());
    }

    /**
     * Factory method pour créer le filtre.
     */
    public static WebClientCorrelationFilter create() {
        return new WebClientCorrelationFilter();
    }
}
