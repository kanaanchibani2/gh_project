package lcl.afx.logging.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lcl.afx.logging.masking.DataMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitaire pour logger avec masquage automatique et format structuré.
 * 
 * Exemple d'utilisation:
 * <pre>
 * LogHelper.controller(log).start("POST", "/api/transfers", "TransferController");
 * LogHelper.controller(log).request(requestBody);
 * LogHelper.controller(log).response(responseBody);
 * LogHelper.controller(log).end("POST", "/api/transfers", "TransferController");
 * </pre>
 */
public class LogHelper {

    private static final DataMasker masker = new DataMasker();
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ══════════════════════════════════════════════════════════════════════════

    public static ControllerLogHelper controller(Logger log) {
        return new ControllerLogHelper(log);
    }

    public static ValidationLogHelper validation(Logger log) {
        return new ValidationLogHelper(log);
    }

    public static DatabaseLogHelper database(Logger log) {
        return new DatabaseLogHelper(log);
    }

    public static CicsLogHelper cics(Logger log, String transactionName) {
        return new CicsLogHelper(log, transactionName);
    }

    public static ApiLogHelper api(Logger log, String serviceName) {
        return new ApiLogHelper(log, serviceName);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════════

    public static String mask(String input) {
        return masker.mask(input);
    }

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        try {
            return masker.mask(mapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            return masker.mask(obj.toString());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONTROLLER LOG HELPER (étapes 1, 2, 7, 8, 9)
    // ══════════════════════════════════════════════════════════════════════════

    public static class ControllerLogHelper {
        private final Logger log;

        ControllerLogHelper(Logger log) {
            this.log = log;
        }

        /** Étape 1: Début du contrôleur */
        public void start(String httpMethod, String url, String controllerName) {
            log.info("{} {} {} Start", httpMethod, url, controllerName);
        }

        /** Étape 2: Log de la requête métier */
        public void request(Object body) {
            log.info("Request: {}", toJson(body));
        }

        /** Étape 7: Réponse API client */
        public void response(Object body) {
            log.info("Response: {}", toJson(body));
        }

        /** Étape 8: Fin du contrôleur */
        public void end(String httpMethod, String url, String controllerName) {
            log.info("{} {} {} End", httpMethod, url, controllerName);
        }

        /** Étape 9: Fin de la requête métier */
        public void endRequest(Object body) {
            log.info("END Request: {}", toJson(body));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VALIDATION LOG HELPER (étape 3)
    // ══════════════════════════════════════════════════════════════════════════

    public static class ValidationLogHelper {
        private final Logger log;

        ValidationLogHelper(Logger log) {
            this.log = log;
        }

        public void start(String validationName) {
            log.info("Validation {} Start", validationName);
        }

        public void success(String validationName) {
            log.info("Validation {} Success", validationName);
        }

        public void failed(String validationName, String reason) {
            log.warn("Validation {} Failed: {}", validationName, mask(reason));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATABASE LOG HELPER (étape 4)
    // ══════════════════════════════════════════════════════════════════════════

    public static class DatabaseLogHelper {
        private final Logger log;

        DatabaseLogHelper(Logger log) {
            this.log = log;
        }

        public void start(String operation, Object params) {
            log.info("[DB] {} params={}", operation, toJson(params));
        }

        public void start(String operation) {
            log.info("[DB] {}", operation);
        }

        public void success(String operation, long timeMs, Object result) {
            log.info("[DB] {} Success (time={}ms) result={}", operation, timeMs, toJson(result));
        }

        public void success(String operation, long timeMs, int rowCount) {
            log.info("[DB] {} Success (time={}ms) rowCount={}", operation, timeMs, rowCount);
        }

        public void failed(String operation, long timeMs, String error) {
            log.error("[DB] {} Failed (time={}ms): {}", operation, timeMs, mask(error));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CICS LOG HELPER (étape 5)
    // ══════════════════════════════════════════════════════════════════════════

    public static class CicsLogHelper {
        private final Logger log;
        private final String txName;

        CicsLogHelper(Logger log, String txName) {
            this.log = log;
            this.txName = txName;
        }

        /** Log 1: Context */
        public void context(String correlationId, String userId) {
            log.info("[{}] Context: correlationId={}, userId={}", txName, correlationId, userId);
        }

        /** Log 2: Input */
        public void input(Object commarea) {
            log.info("[{}] input: {}Commarea.Input {}", txName, txName, toJson(commarea));
        }

        /** Log 3: Output */
        public void output(Object commarea) {
            log.info("[{}] output: {}Commarea.Output {}", txName, txName, toJson(commarea));
        }

        /** Log 4: InfosImportantes */
        public void infos(String codeRetour, String statut, String reference) {
            log.info("[{}] InfosImportantes: codeRetour={}, statut={}, reference={}", 
                txName, codeRetour, statut, reference);
        }

        public void infos(Object infos) {
            log.info("[{}] InfosImportantes: {}", txName, toJson(infos));
        }

        public void error(String error, long timeMs) {
            log.error("[{}] Error: {} (time={}ms)", txName, mask(error), timeMs);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // API LOG HELPER (étape 6)
    // ══════════════════════════════════════════════════════════════════════════

    public static class ApiLogHelper {
        private final Logger log;
        private final String serviceName;

        ApiLogHelper(Logger log, String serviceName) {
            this.log = log;
            this.serviceName = serviceName;
        }

        /** Log 1: Context */
        public void context(String operation, String correlationId) {
            log.info("[API] Context: service={}, operation={}, correlationId={}", 
                serviceName, operation, correlationId);
        }

        /** Log 2: Requête */
        public void request(String httpMethod, String url, Object body) {
            log.info("[API][{} {}] [REQUETE] {}", httpMethod, url, toJson(body));
        }

        /** Log 3: Réponse */
        public void response(String httpMethod, String url, int status, Object body) {
            log.info("[API][{} {}] [REPONSE] {} {}", httpMethod, url, status, toJson(body));
        }

        /** Log 4: InfosImportantes */
        public void infos(int status, long timeMs) {
            log.info("[API] InfosImportantes: service={}, statut={}, tempsReponseMs={}", 
                serviceName, status, timeMs);
        }

        public void infos(int status, long timeMs, String error) {
            log.info("[API] InfosImportantes: service={}, statut={}, tempsReponseMs={}, erreur={}", 
                serviceName, status, timeMs, mask(error));
        }

        public void error(String httpMethod, String url, String error) {
            log.error("[API][{} {}] [REPONSE] ERROR {}", httpMethod, url, mask(error));
        }
    }
}
