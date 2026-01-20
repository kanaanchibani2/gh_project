package lcl.afx.logging.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration pour le logging centralisé.
 * 
 * <p>Configuration YAML:</p>
 * <pre>
 * afx:
 *   logging:
 *     enabled: true
 *     masking:
 *       enabled: true
 *     aspect:
 *       enabled: true
 *       performance-threshold-ms: 1000
 *     correlation:
 *       enabled: true
 *       header-name: X-Correlation-ID
 *       generate-if-missing: true
 *     propagation:
 *       rest-template: true
 *       rest-client: true
 *       web-client: true
 *       feign: true
 * </pre>
 */
@ConfigurationProperties(prefix = "afx.logging")
public class LoggingProperties {

    /**
     * Active ou désactive le logging centralisé.
     */
    private boolean enabled = true;

    /**
     * Nom du service (utilisé dans les logs).
     */
    private String serviceName;

    /**
     * Configuration du masquage.
     */
    private MaskingProperties masking = new MaskingProperties();

    /**
     * Configuration de l'aspect AOP.
     */
    private AspectProperties aspect = new AspectProperties();

    /**
     * Configuration du correlation ID.
     */
    private CorrelationProperties correlation = new CorrelationProperties();

    /**
     * Configuration de la propagation inter-services.
     */
    private PropagationProperties propagation = new PropagationProperties();

    // ══════════════════════════════════════════════════════════════════════════
    // CLASSES IMBRIQUÉES
    // ══════════════════════════════════════════════════════════════════════════

    public static class MaskingProperties {
        /**
         * Active le masquage des données sensibles.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class AspectProperties {
        /**
         * Active l'aspect AOP pour @PaymentLog.
         */
        private boolean enabled = true;

        /**
         * Seuil de performance par défaut (ms).
         */
        private long performanceThresholdMs = 1000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getPerformanceThresholdMs() {
            return performanceThresholdMs;
        }

        public void setPerformanceThresholdMs(long performanceThresholdMs) {
            this.performanceThresholdMs = performanceThresholdMs;
        }
    }

    public static class CorrelationProperties {
        /**
         * Active le filtre correlation ID.
         */
        private boolean enabled = true;

        /**
         * Nom du header HTTP pour le correlation ID.
         */
        private String headerName = "X-Correlation-ID";

        /**
         * Génère un correlation ID si absent.
         */
        private boolean generateIfMissing = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public boolean isGenerateIfMissing() {
            return generateIfMissing;
        }

        public void setGenerateIfMissing(boolean generateIfMissing) {
            this.generateIfMissing = generateIfMissing;
        }
    }

    public static class PropagationProperties {
        /**
         * Active l'intercepteur RestTemplate.
         */
        private boolean restTemplate = true;

        /**
         * Active l'intercepteur RestClient.
         */
        private boolean restClient = true;

        /**
         * Active le filtre WebClient.
         */
        private boolean webClient = true;

        /**
         * Active l'intercepteur Feign.
         */
        private boolean feign = true;

        public boolean isRestTemplate() {
            return restTemplate;
        }

        public void setRestTemplate(boolean restTemplate) {
            this.restTemplate = restTemplate;
        }

        public boolean isRestClient() {
            return restClient;
        }

        public void setRestClient(boolean restClient) {
            this.restClient = restClient;
        }

        public boolean isWebClient() {
            return webClient;
        }

        public void setWebClient(boolean webClient) {
            this.webClient = webClient;
        }

        public boolean isFeign() {
            return feign;
        }

        public void setFeign(boolean feign) {
            this.feign = feign;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GETTERS / SETTERS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public MaskingProperties getMasking() {
        return masking;
    }

    public void setMasking(MaskingProperties masking) {
        this.masking = masking;
    }

    public AspectProperties getAspect() {
        return aspect;
    }

    public void setAspect(AspectProperties aspect) {
        this.aspect = aspect;
    }

    public CorrelationProperties getCorrelation() {
        return correlation;
    }

    public void setCorrelation(CorrelationProperties correlation) {
        this.correlation = correlation;
    }

    public PropagationProperties getPropagation() {
        return propagation;
    }

    public void setPropagation(PropagationProperties propagation) {
        this.propagation = propagation;
    }
}
