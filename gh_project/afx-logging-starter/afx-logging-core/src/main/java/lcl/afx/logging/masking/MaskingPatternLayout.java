package lcl.afx.logging.masking;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Layout Logback personnalisé qui applique le masquage des données sensibles
 * sur les logs formatés en texte (console, fichier texte).
 * 
 * <p>Utilisation dans logback-spring.xml:</p>
 * <pre>
 * &lt;appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender"&gt;
 *     &lt;encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder"&gt;
 *         &lt;layout class="lcl.afx.logging.masking.MaskingPatternLayout"&gt;
 *             &lt;pattern&gt;%d{HH:mm:ss.SSS} %-5level [%X{correlation_id:-}] %logger{36} - %msg%n&lt;/pattern&gt;
 *         &lt;/layout&gt;
 *     &lt;/encoder&gt;
 * &lt;/appender&gt;
 * </pre>
 */
public class MaskingPatternLayout extends PatternLayout {

    private final DataMasker dataMasker = new DataMasker();
    private boolean maskingEnabled = true;

    @Override
    public String doLayout(ILoggingEvent event) {
        // 1. Formater le message avec le pattern standard
        String message = super.doLayout(event);
        
        // 2. Appliquer le masquage si activé
        if (!maskingEnabled || message == null) {
            return message;
        }
        
        return dataMasker.mask(message);
    }

    /**
     * Active ou désactive le masquage.
     * Configurable via XML: &lt;maskingEnabled&gt;true&lt;/maskingEnabled&gt;
     */
    public void setMaskingEnabled(boolean maskingEnabled) {
        this.maskingEnabled = maskingEnabled;
    }

    public boolean isMaskingEnabled() {
        return maskingEnabled;
    }
}
