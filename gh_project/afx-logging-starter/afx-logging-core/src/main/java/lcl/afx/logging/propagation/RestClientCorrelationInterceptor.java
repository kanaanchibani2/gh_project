package lcl.afx.logging.propagation;

import lcl.afx.logging.mdc.MdcKeys;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Intercepteur RestClient (Spring Boot 3.2+) qui propage le correlation ID
 * et autres informations MDC dans les appels HTTP sortants.
 * 
 * <p>Utilisation:</p>
 * <pre>
 * &#64;Bean
 * public RestClient restClient(RestClientCorrelationInterceptor interceptor) {
 *     return RestClient.builder()
 *         .requestInterceptor(interceptor)
 *         .build();
 * }
 * </pre>
 */
public class RestClientCorrelationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {

        // Lire du MDC et ajouter aux headers sortants
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null) {
            request.getHeaders().set(MdcKeys.HEADER_CORRELATION_ID, correlationId);
        }

        String transactionId = MDC.get(MdcKeys.TRANSACTION_ID);
        if (transactionId != null) {
            request.getHeaders().set(MdcKeys.HEADER_TRANSACTION_ID, transactionId);
        }

        String userId = MDC.get(MdcKeys.USER_ID);
        if (userId != null) {
            request.getHeaders().set(MdcKeys.HEADER_USER_ID, userId);
        }

        return execution.execute(request, body);
    }
}
