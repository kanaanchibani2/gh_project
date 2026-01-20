package lcl.afx.logging.masking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service de masquage des données sensibles conformément aux normes RGPD et PCI-DSS.
 * 
 * <p>Données masquées:</p>
 * <ul>
 *   <li>PAN (numéros de carte bancaire)</li>
 *   <li>IBAN</li>
 *   <li>Emails</li>
 *   <li>Numéros de téléphone</li>
 *   <li>CVV/CVC</li>
 * </ul>
 */
public class DataMasker {

    private final List<MaskingRule> rules;

    public DataMasker() {
        this.rules = initializeDefaultRules();
    }

    /**
     * Applique toutes les règles de masquage sur l'input.
     *
     * @param input La chaîne à masquer
     * @return La chaîne avec les données sensibles masquées
     */
    public String mask(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String result = input;
        for (MaskingRule rule : rules) {
            result = rule.pattern().matcher(result).replaceAll(rule.replacement());
        }
        return result;
    }

    /**
     * Initialise les règles de masquage par défaut.
     */
    private List<MaskingRule> initializeDefaultRules() {
        List<MaskingRule> defaultRules = new ArrayList<>();

        // ══════════════════════════════════════════════════════════════════════
        // PAN - Numéros de carte bancaire (Visa, Mastercard, Amex, etc.)
        // Détecte : 4532015112830366, 4532-0151-1283-0366
        // Masque : 453201******0366
        // ══════════════════════════════════════════════════════════════════════
        defaultRules.add(new MaskingRule(
            Pattern.compile("\\b([3-6]\\d{5})\\d{4,9}(\\d{4})\\b"),
            "$1******$2"
        ));

        // PAN avec tirets ou espaces
        defaultRules.add(new MaskingRule(
            Pattern.compile("\\b([3-6]\\d{3})[- ]?(\\d{4})[- ]?(\\d{4})[- ]?(\\d{4})\\b"),
            "$1-****-****-$4"
        ));

        // ══════════════════════════════════════════════════════════════════════
        // IBAN - International Bank Account Number
        // Détecte : FR7630006000011234567890189
        // Masque : FR76************0189
        // ══════════════════════════════════════════════════════════════════════
        defaultRules.add(new MaskingRule(
            Pattern.compile("\\b([A-Z]{2}\\d{2})[A-Z0-9]{8,26}([A-Z0-9]{4})\\b"),
            "$1************$2"
        ));

        // ══════════════════════════════════════════════════════════════════════
        // Email
        // Détecte : jean.dupont@email.com
        // Masque : j***@email.com
        // ══════════════════════════════════════════════════════════════════════
        defaultRules.add(new MaskingRule(
            Pattern.compile("\\b([a-zA-Z0-9])[a-zA-Z0-9._%+-]*@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\b"),
            "$1***@$2"
        ));

        // ══════════════════════════════════════════════════════════════════════
        // Téléphone français
        // Détecte : +33612345678, 0612345678, 06 12 34 56 78
        // Masque : +336******78, 06******78
        // ══════════════════════════════════════════════════════════════════════
        defaultRules.add(new MaskingRule(
            Pattern.compile("\\b(\\+?33|0)([1-9])(\\d{2})[\\s.-]?(\\d{2})[\\s.-]?(\\d{2})[\\s.-]?(\\d{2})\\b"),
            "$1$2******$6"
        ));

        // ══════════════════════════════════════════════════════════════════════
        // CVV/CVC dans un contexte JSON ou texte
        // Détecte : "cvv": "123", cvv: 456, cvc=789
        // Masque : cvv:***
        // ══════════════════════════════════════════════════════════════════════
        defaultRules.add(new MaskingRule(
            Pattern.compile("(?i)(cvv|cvc|cvn)[\"':\\s=]*(\\d{3,4})"),
            "$1:***"
        ));

        // ══════════════════════════════════════════════════════════════════════
        // Numéro de sécurité sociale (France)
        // Détecte : 1 85 12 75 108 123 45
        // Masque : 1 85 ** ** *** *** **
        // ══════════════════════════════════════════════════════════════════════
        defaultRules.add(new MaskingRule(
            Pattern.compile("\\b([12])\\s?(\\d{2})\\s?(\\d{2})\\s?(\\d{2})\\s?(\\d{3})\\s?(\\d{3})\\s?(\\d{2})\\b"),
            "$1 $2 ** ** *** *** **"
        ));

        return defaultRules;
    }

    /**
     * Ajoute une règle de masquage personnalisée.
     *
     * @param pattern Le pattern regex à détecter
     * @param replacement Le remplacement à appliquer
     */
    public void addRule(Pattern pattern, String replacement) {
        rules.add(new MaskingRule(pattern, replacement));
    }

    /**
     * Règle de masquage.
     */
    public record MaskingRule(Pattern pattern, String replacement) {}
}
