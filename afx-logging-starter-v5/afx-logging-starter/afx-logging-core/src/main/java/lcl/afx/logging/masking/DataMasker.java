package lcl.afx.logging.masking;

import java.util.regex.Pattern;

/**
 * Utilitaire pour masquer les données sensibles dans les logs.
 * Compatible RGPD et PCI-DSS.
 */
public class DataMasker {

    // IBAN : garde les 4 premiers et 4 derniers caractères
    private static final Pattern IBAN_PATTERN = Pattern.compile(
        "\\b([A-Z]{2}\\d{2})([A-Z0-9]{4,})([A-Z0-9]{4})\\b"
    );

    // Email : masque la partie locale
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\b"
    );

    // Téléphone français
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b(\\+33|0033|0)(\\d{1})(\\d{6})(\\d{2})\\b"
    );

    // Carte bancaire (16 chiffres)
    private static final Pattern CARD_PATTERN = Pattern.compile(
        "\\b(\\d{4})(\\d{8})(\\d{4})\\b"
    );

    // Numéro de sécurité sociale
    private static final Pattern SSN_PATTERN = Pattern.compile(
        "\\b([12])\\s?(\\d{2})\\s?(\\d{2})\\s?(\\d{2})\\s?(\\d{3})\\s?(\\d{3})\\s?(\\d{2})\\b"
    );

    public String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // Masquer IBAN
        result = IBAN_PATTERN.matcher(result).replaceAll("$1************$3");

        // Masquer Email
        result = EMAIL_PATTERN.matcher(result).replaceAll(match -> {
            String local = match.group(1);
            String domain = match.group(2);
            String maskedLocal = local.charAt(0) + "***";
            return maskedLocal + "@" + domain;
        });

        // Masquer Téléphone
        result = PHONE_PATTERN.matcher(result).replaceAll("$1$2******$4");

        // Masquer Carte bancaire
        result = CARD_PATTERN.matcher(result).replaceAll("$1********$3");

        // Masquer NSS
        result = SSN_PATTERN.matcher(result).replaceAll("$1 ** ** ** *** *** **");

        return result;
    }
}
