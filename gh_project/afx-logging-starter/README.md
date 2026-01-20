# AFX Logging Spring Boot Starter

Spring Boot Starter pour la centralisation des logs avec masquage automatique RGPD/PCI-DSS.

## 🚀 Installation

### Ajouter la dépendance Maven

```xml
<dependency>
    <groupId>lcl.afx</groupId>
    <artifactId>afx-logging-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Configuration minimale

```properties
# application.properties
spring.application.name=mon-service
```

## 📋 Fonctionnalités

- ✅ **Masquage automatique RGPD/PCI-DSS** : IBAN, PAN, email, téléphone, CVV
- ✅ **Correlation ID** : Traçabilité bout-en-bout cross-services
- ✅ **Logging AOP** : Annotation `@PaymentLog` pour logging automatique
- ✅ **Format JSON** : Compatible Elasticsearch/ELK
- ✅ **Audit Trail** : Fichier séparé pour l'audit
- ✅ **Propagation** : RestTemplate, RestClient, WebClient, Feign

## 🔧 Configuration

```properties
# application.properties

# Activer/désactiver le starter
afx.logging.enabled=true

# Masquage
afx.logging.masking.enabled=true

# Aspect AOP
afx.logging.aspect.enabled=true
afx.logging.aspect.performance-threshold-ms=1000

# Correlation ID
afx.logging.correlation.enabled=true
afx.logging.correlation.header-name=X-Correlation-ID
afx.logging.correlation.generate-if-missing=true

# Propagation inter-services
afx.logging.propagation.rest-template=true
afx.logging.propagation.rest-client=true
afx.logging.propagation.web-client=true
afx.logging.propagation.feign=true
```

## 📝 Utilisation

### Annotation @PaymentLog

```java
@Service
public class PaymentService {

    @PaymentLog(operation = "SEPA_TRANSFER", auditEnabled = true)
    public TransferResult executeTransfer(TransferRequest request) {
        // Logs automatiques : ENTRY, EXIT, SLOW, ERROR
        return result;
    }

    @NoLogging(reason = "Méthode interne fréquente")
    private boolean validateIban(String iban) {
        return iban != null;
    }
}
```

### Configuration Logback

```xml
<!-- logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <springProperty scope="context" name="SERVICE_NAME" 
                    source="spring.application.name" 
                    defaultValue="unknown-service"/>

    <appender name="stdout" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="lcl.afx.logging.masking.MaskingPatternLayout">
                <pattern>%d{HH:mm:ss.SSS} %-5level [${SERVICE_NAME}] [%X{correlation_id:-}] [%X{operation:-}] %logger{36} - %msg%n</pattern>
            </layout>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="stdout"/>
    </root>

</configuration>
```

## 📊 Exemple de logs

### Avant (sans masquage)
```
INFO - Virement de FR7630006000011234567890189 vers DE89370400440532013000
INFO - Email client: jean.dupont@email.com
INFO - Carte: 4532015112830366
```

### Après (avec masquage)
```
INFO [abc-123] [SEPA_TRANSFER] - ▶ ENTRY params={debtorIban=FR76************0189, amount=1500.00}
INFO [abc-123] [SEPA_TRANSFER] - Virement de FR76************0189 vers DE89************3000
INFO [abc-123] [SEPA_TRANSFER] - Email client: j***@email.com
INFO [abc-123] [SEPA_TRANSFER] - Carte: 453201******0366
INFO [abc-123] [SEPA_TRANSFER] - ◀ EXIT time=200ms result={status=SUCCESS}
```

## 📦 Structure des modules

```
afx-logging-starter/
├── pom.xml (parent)
├── afx-logging-core/                    # Code : annotations, masquage, filtres, aspects
│   └── src/main/java/lcl/afx/logging/
│       ├── annotation/                  # @PaymentLog, @NoLogging
│       ├── masking/                     # DataMasker, MaskingPatternLayout, MaskingJsonLayout
│       ├── filter/                      # CorrelationIdFilter
│       ├── aspect/                      # PaymentLoggingAspect
│       └── propagation/                 # Intercepteurs HTTP
├── afx-logging-autoconfigure/           # Auto-configuration Spring Boot
│   └── src/main/java/lcl/afx/logging/autoconfigure/
│       ├── LoggingProperties.java
│       └── LoggingAutoConfiguration.java
└── afx-logging-spring-boot-starter/     # Agrégateur de dépendances
    └── pom.xml
```

## 🔗 Liens

- **Documentation** : [Confluence](https://confluence.lcl.fr/display/AFX/Logging)
- **Repository** : [GitLab](https://gitlab.lcl.fr/afx/afx-logging-starter)

## 📄 License

Propriétaire LCL - Usage interne uniquement.
