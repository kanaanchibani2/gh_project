package lcl.afx.logging.mdc;

/**
 * Constantes pour les clés MDC et headers HTTP.
 */
public final class MdcKeys {

    private MdcKeys() {
        // Utility class
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CLÉS MDC
    // ══════════════════════════════════════════════════════════════════════════
    public static final String CORRELATION_ID = "correlation_id";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String USER_ID = "user_id";
    public static final String CLIENT_IP = "client_ip";
    public static final String REQUEST_URI = "request_uri";
    public static final String REQUEST_METHOD = "request_method";
    public static final String OPERATION = "operation";
    public static final String OPERATION_ID = "operation_id";

    // ══════════════════════════════════════════════════════════════════════════
    // HEADERS HTTP
    // ══════════════════════════════════════════════════════════════════════════
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_TRANSACTION_ID = "X-Transaction-ID";
    public static final String HEADER_USER_ID = "X-User-ID";
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
}
