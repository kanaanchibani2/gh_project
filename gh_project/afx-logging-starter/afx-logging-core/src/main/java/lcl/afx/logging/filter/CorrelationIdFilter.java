package lcl.afx.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lcl.afx.logging.mdc.MdcKeys;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtre HTTP qui enrichit le MDC avec le contexte de la requête.
 * 
 * <p>Ce filtre:</p>
 * <ul>
 *   <li>Génère ou lit le correlation ID depuis les headers</li>
 *   <li>Extrait l'IP client (proxy-aware)</li>
 *   <li>Enrichit le MDC avec request_uri, request_method, user_id</li>
 *   <li>Propage le correlation ID dans la réponse</li>
 * </ul>
 * 
 * <p>S'exécute en premier (HIGHEST_PRECEDENCE) pour que tous les logs
 * de la requête bénéficient du contexte MDC.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    // Configuration
    private boolean includeClientIp = true;
    private boolean includeRequestUri = true;
    private boolean generateIfMissing = true;
    private String correlationIdHeader = MdcKeys.HEADER_CORRELATION_ID;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
                                    throws ServletException, IOException {
        try {
            // 1. Enrichir le MDC avec le contexte de la requête
            setupMdc(request);

            // 2. Propager le correlation ID dans la réponse
            String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
            if (correlationId != null) {
                response.setHeader(correlationIdHeader, correlationId);
            }

            // 3. Continuer la chaîne de filtres
            filterChain.doFilter(request, response);

        } finally {
            // 4. TOUJOURS nettoyer le MDC (éviter les fuites entre threads)
            clearMdc();
        }
    }

    /**
     * Configure le MDC avec le contexte de la requête.
     */
    private void setupMdc(HttpServletRequest request) {
        // ══════════════════════════════════════════════════════════════════════
        // Correlation ID : lire des headers ou générer
        // ══════════════════════════════════════════════════════════════════════
        String correlationId = request.getHeader(correlationIdHeader);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(MdcKeys.HEADER_REQUEST_ID);
        }
        if ((correlationId == null || correlationId.isBlank()) && generateIfMissing) {
            correlationId = UUID.randomUUID().toString();
        }
        if (correlationId != null) {
            MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        }

        // ══════════════════════════════════════════════════════════════════════
        // Transaction ID (optionnel, depuis header)
        // ══════════════════════════════════════════════════════════════════════
        String transactionId = request.getHeader(MdcKeys.HEADER_TRANSACTION_ID);
        if (transactionId != null && !transactionId.isBlank()) {
            MDC.put(MdcKeys.TRANSACTION_ID, transactionId);
        }

        // ══════════════════════════════════════════════════════════════════════
        // Client IP (proxy-aware)
        // ══════════════════════════════════════════════════════════════════════
        if (includeClientIp) {
            MDC.put(MdcKeys.CLIENT_IP, extractClientIp(request));
        }

        // ══════════════════════════════════════════════════════════════════════
        // Request URI et Method
        // ══════════════════════════════════════════════════════════════════════
        if (includeRequestUri) {
            MDC.put(MdcKeys.REQUEST_URI, request.getRequestURI());
            MDC.put(MdcKeys.REQUEST_METHOD, request.getMethod());
        }

        // ══════════════════════════════════════════════════════════════════════
        // User ID (depuis Spring Security si disponible)
        // ══════════════════════════════════════════════════════════════════════
        extractUserId();
    }

    /**
     * Extrait l'IP client en tenant compte des proxies et load balancers.
     */
    private String extractClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For peut contenir plusieurs IPs (client, proxy1, proxy2)
                // On prend la première (le client original)
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Extrait le user ID depuis Spring Security si disponible.
     */
    private void extractUserId() {
        try {
            // Utilisation de reflection pour éviter la dépendance obligatoire
            Class<?> securityContextHolderClass = Class.forName(
                "org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolderClass.getMethod("getContext").invoke(null);
            
            if (context != null) {
                Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
                if (authentication != null) {
                    Boolean isAuthenticated = (Boolean) authentication.getClass()
                        .getMethod("isAuthenticated").invoke(authentication);
                    Object principal = authentication.getClass()
                        .getMethod("getPrincipal").invoke(authentication);
                    
                    if (Boolean.TRUE.equals(isAuthenticated) 
                        && principal != null 
                        && !"anonymousUser".equals(principal.toString())) {
                        
                        String name = (String) authentication.getClass()
                            .getMethod("getName").invoke(authentication);
                        if (name != null && !name.isBlank()) {
                            MDC.put(MdcKeys.USER_ID, name);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // Spring Security pas sur le classpath - ignorer silencieusement
        } catch (Exception e) {
            // Autre erreur - ignorer silencieusement
        }
    }

    /**
     * Nettoie le MDC pour éviter les fuites de données entre requêtes.
     */
    private void clearMdc() {
        MDC.remove(MdcKeys.CORRELATION_ID);
        MDC.remove(MdcKeys.TRANSACTION_ID);
        MDC.remove(MdcKeys.CLIENT_IP);
        MDC.remove(MdcKeys.REQUEST_URI);
        MDC.remove(MdcKeys.REQUEST_METHOD);
        MDC.remove(MdcKeys.USER_ID);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Setters pour configuration
    // ══════════════════════════════════════════════════════════════════════════

    public void setIncludeClientIp(boolean includeClientIp) {
        this.includeClientIp = includeClientIp;
    }

    public void setIncludeRequestUri(boolean includeRequestUri) {
        this.includeRequestUri = includeRequestUri;
    }

    public void setGenerateIfMissing(boolean generateIfMissing) {
        this.generateIfMissing = generateIfMissing;
    }

    public void setCorrelationIdHeader(String correlationIdHeader) {
        this.correlationIdHeader = correlationIdHeader;
    }

    // Getters
    public boolean isIncludeClientIp() {
        return includeClientIp;
    }

    public boolean isIncludeRequestUri() {
        return includeRequestUri;
    }

    public boolean isGenerateIfMissing() {
        return generateIfMissing;
    }

    public String getCorrelationIdHeader() {
        return correlationIdHeader;
    }
}
