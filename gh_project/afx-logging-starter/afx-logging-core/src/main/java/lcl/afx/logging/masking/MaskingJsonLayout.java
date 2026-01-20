package lcl.afx.logging.masking;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Layout Logback personnalisé qui génère des logs au format JSON
 * avec masquage des données sensibles, compatible Elasticsearch/ELK.
 * 
 * <p>Utilisation dans logback-spring.xml:</p>
 * <pre>
 * &lt;appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender"&gt;
 *     &lt;encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder"&gt;
 *         &lt;layout class="lcl.afx.logging.masking.MaskingJsonLayout"&gt;
 *             &lt;serviceName&gt;${SERVICE_NAME}&lt;/serviceName&gt;
 *             &lt;environment&gt;${ENVIRONMENT}&lt;/environment&gt;
 *             &lt;includeMdc&gt;true&lt;/includeMdc&gt;
 *             &lt;maskingEnabled&gt;true&lt;/maskingEnabled&gt;
 *         &lt;/layout&gt;
 *     &lt;/encoder&gt;
 * &lt;/appender&gt;
 * </pre>
 */
public class MaskingJsonLayout extends LayoutBase<ILoggingEvent> {

    private static final DateTimeFormatter ISO_FORMATTER = 
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataMasker dataMasker = new DataMasker();

    // Configuration
    private String serviceName = "unknown-service";
    private String environment = "unknown";
    private boolean includeStackTrace = true;
    private boolean includeMdc = true;
    private boolean maskingEnabled = true;
    private int maxStackTraceDepth = 50;

    @Override
    public String doLayout(ILoggingEvent event) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            // ══════════════════════════════════════════════════════════════════
            // Timestamp ISO 8601 (compatible Elasticsearch)
            // ══════════════════════════════════════════════════════════════════
            root.put("@timestamp", ISO_FORMATTER.format(
                Instant.ofEpochMilli(event.getTimeStamp())));

            // ══════════════════════════════════════════════════════════════════
            // Métadonnées du log
            // ══════════════════════════════════════════════════════════════════
            root.put("level", event.getLevel().toString());
            root.put("logger", event.getLoggerName());
            root.put("thread", event.getThreadName());

            // ══════════════════════════════════════════════════════════════════
            // Identité du service
            // ══════════════════════════════════════════════════════════════════
            root.put("service", serviceName);
            root.put("environment", environment);

            // ══════════════════════════════════════════════════════════════════
            // MDC (correlation_id, user_id, client_ip, operation, etc.)
            // ══════════════════════════════════════════════════════════════════
            if (includeMdc) {
                Map<String, String> mdc = event.getMDCPropertyMap();
                if (mdc != null && !mdc.isEmpty()) {
                    ObjectNode contextNode = root.putObject("context");
                    mdc.forEach((key, value) -> {
                        // Masquer aussi les valeurs MDC
                        String maskedValue = maskingEnabled ? dataMasker.mask(value) : value;
                        contextNode.put(key, maskedValue);
                    });
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // Message (masqué)
            // ══════════════════════════════════════════════════════════════════
            String message = event.getFormattedMessage();
            if (message != null) {
                root.put("message", maskingEnabled ? dataMasker.mask(message) : message);
            }

            // ══════════════════════════════════════════════════════════════════
            // Exception (si présente)
            // ══════════════════════════════════════════════════════════════════
            IThrowableProxy throwable = event.getThrowableProxy();
            if (throwable != null && includeStackTrace) {
                ObjectNode exNode = root.putObject("exception");
                exNode.put("class", throwable.getClassName());

                // Masquer aussi le message d'exception
                if (throwable.getMessage() != null) {
                    exNode.put("message", maskingEnabled
                        ? dataMasker.mask(throwable.getMessage())
                        : throwable.getMessage());
                }

                // Stack trace
                StackTraceElementProxy[] frames = throwable.getStackTraceElementProxyArray();
                if (frames != null && frames.length > 0) {
                    ArrayNode stackTrace = exNode.putArray("stack_trace");
                    int depth = Math.min(frames.length, maxStackTraceDepth);
                    for (int i = 0; i < depth; i++) {
                        stackTrace.add(frames[i].getSTEAsString());
                    }
                }
            }

            return objectMapper.writeValueAsString(root) + System.lineSeparator();

        } catch (JsonProcessingException e) {
            // Fallback en cas d'erreur de sérialisation
            return String.format("{\"error\":\"JSON serialization failed\",\"message\":\"%s\"}%n",
                event.getFormattedMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Setters pour configuration Logback XML
    // ══════════════════════════════════════════════════════════════════════════

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setIncludeStackTrace(boolean includeStackTrace) {
        this.includeStackTrace = includeStackTrace;
    }

    public void setIncludeMdc(boolean includeMdc) {
        this.includeMdc = includeMdc;
    }

    public void setMaskingEnabled(boolean maskingEnabled) {
        this.maskingEnabled = maskingEnabled;
    }

    public void setMaxStackTraceDepth(int maxStackTraceDepth) {
        this.maxStackTraceDepth = maxStackTraceDepth;
    }

    // Getters
    public String getServiceName() {
        return serviceName;
    }

    public String getEnvironment() {
        return environment;
    }

    public boolean isIncludeStackTrace() {
        return includeStackTrace;
    }

    public boolean isIncludeMdc() {
        return includeMdc;
    }

    public boolean isMaskingEnabled() {
        return maskingEnabled;
    }

    public int getMaxStackTraceDepth() {
        return maxStackTraceDepth;
    }
}
