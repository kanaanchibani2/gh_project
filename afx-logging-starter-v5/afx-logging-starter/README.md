# AFX Logging Starter - Enchaînement Logique des Logs

Ce starter ajoute l'enchaînement logique des logs par-dessus le **Layout commun LCL existant** (`com.cl.logs.layouts.Layout`).

## 📦 Installation

```xml
<dependency>
    <groupId>lcl.afx</groupId>
    <artifactId>afx-logging-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🎯 Annotations disponibles

| Annotation | Étape | Description |
|------------|-------|-------------|
| `@LogFlow` | 1,2,7,8,9 | Contrôleur: Start/Request/Response/End/END Request |
| `@LogValidation` | 3 | Validation: Start/Success/Failed |
| `@LogDatabase` | 4 | Base de données: operation/Success/Failed |
| `@LogCics` | 5 | CICS: Context/input/output/InfosImportantes |
| `@LogApi` | 6 | API externe: Context/REQUETE/REPONSE/InfosImportantes |

## 📝 Exemple d'utilisation

```java
@RestController
@RequestMapping("/api/v1")
@LogFlow  // Étapes 1, 2, 7, 8, 9 automatiques
public class TransferController {

    @PostMapping("/transfers")
    public ResponseEntity<TransferResult> transfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.execute(request));
    }
}

@Service
public class TransferService {

    public TransferResult execute(TransferRequest request) {
        // Étape 3: Validation
        validate(request);
        
        // Étape 4: Base de données
        BigDecimal balance = getBalance(request.getDebtorIban());
        
        // Étape 5: Appel CICS
        CicsResponse cics = callCics(request);
        
        // Étape 6: Appel API externe
        sendNotification(request.getEmail());
        
        return new TransferResult("SUCCESS");
    }

    @LogValidation("REQUEST")
    public void validate(TransferRequest request) {
        // Validation...
    }

    @LogDatabase("SELECT_BALANCE")
    public BigDecimal getBalance(String iban) {
        // Requête DB...
    }

    @LogCics("KEXX")
    public CicsResponse callCics(TransferRequest request) {
        // Appel CICS...
    }

    @LogApi("notification-service")
    public void sendNotification(String email) {
        // Appel REST externe...
    }
}
```

## 📊 Logs générés

```
POST /api/v1/transfers TransferController Start
Request: {"debtorIban":"FR76************0189","amount":1500}
Validation REQUEST Start
Validation REQUEST Success
[DB] SELECT_BALANCE params={"iban":"FR76************0189"}
[DB] SELECT_BALANCE Success (time=5ms) result=10000.00
[KEXX] Context: correlationId=abc-123, userId=jean.dupont
[KEXX] input: KEXXCommarea.Input {"iban":"FR76************0189"}
[KEXX] output: KEXXCommarea.Output {"status":"OK"}
[KEXX] InfosImportantes: codeRetour=0000, statut=SUCCESS, reference=REF-456
[API] Context: service=notification-service, operation=sendNotification
[API][POST /notifications] [REQUETE] {"to":"j***@email.com"}
[API][POST /notifications] [REPONSE] 200 {"status":"sent"}
[API] InfosImportantes: service=notification-service, statut=200, tempsReponseMs=89
Response: {"transactionId":"TXN-123","status":"SUCCESS"}
POST /api/v1/transfers TransferController End
END Request: {"debtorIban":"FR76************0189","amount":1500}
```

## ⚙️ Configuration

```properties
# Activer/désactiver globalement
afx.logging.enabled=true

# Activer/désactiver par étape
afx.logging.flow.enabled=true
afx.logging.validation.enabled=true
afx.logging.database.enabled=true
afx.logging.cics.enabled=true
afx.logging.api.enabled=true
```

## 🔧 Utilisation manuelle (sans annotations)

```java
import lcl.afx.logging.util.LogHelper;

@Service
public class MyService {

    private static final Logger log = LoggerFactory.getLogger(MyService.class);

    public void myMethod() {
        // Validation
        LogHelper.validation(log).start("MY_VALIDATION");
        // ... validation logic ...
        LogHelper.validation(log).success("MY_VALIDATION");

        // Database
        LogHelper.database(log).start("SELECT_DATA", Map.of("id", 123));
        // ... db call ...
        LogHelper.database(log).success("SELECT_DATA", 5, result);

        // CICS
        LogHelper.CicsLogHelper cics = LogHelper.cics(log, "KEXX");
        cics.context(correlationId, userId);
        cics.input(commarea);
        cics.output(result);
        cics.infos("0000", "SUCCESS", "REF-123");

        // API
        LogHelper.ApiLogHelper api = LogHelper.api(log, "external-service");
        api.context("sendData", correlationId);
        api.request("POST", "/api/data", body);
        api.response("POST", "/api/data", 200, response);
        api.infos(200, 150);
    }
}
```

## 📋 Masquage RGPD automatique

Toutes les données sensibles sont automatiquement masquées :

| Type | Exemple avant | Exemple après |
|------|---------------|---------------|
| IBAN | FR7630006000011234567890189 | FR76************0189 |
| Email | jean.dupont@email.com | j***@email.com |
| Téléphone | +33612345678 | +336******78 |
| Carte | 4532015112830366 | 4532********0366 |
