package lcl.afx.logging.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration pour l'enchaînement des logs.
 */
@ConfigurationProperties(prefix = "afx.logging")
public class LoggingProperties {

    private boolean enabled = true;

    private Flow flow = new Flow();
    private Validation validation = new Validation();
    private Database database = new Database();
    private Cics cics = new Cics();
    private Api api = new Api();

    // Getters et Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Flow getFlow() { return flow; }
    public void setFlow(Flow flow) { this.flow = flow; }

    public Validation getValidation() { return validation; }
    public void setValidation(Validation validation) { this.validation = validation; }

    public Database getDatabase() { return database; }
    public void setDatabase(Database database) { this.database = database; }

    public Cics getCics() { return cics; }
    public void setCics(Cics cics) { this.cics = cics; }

    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }

    public static class Flow {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Validation {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Database {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Cics {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Api {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
