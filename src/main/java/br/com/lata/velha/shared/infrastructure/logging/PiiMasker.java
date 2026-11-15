package br.com.lata.velha.shared.infrastructure.logging;

import java.util.regex.Pattern;

/**
 * Mascaramento de PII em logs (Story 19.5).
 *
 * Cobre CPF (com/sem pontuação), e-mail, JWT e pares
 * password/senha/token/authorization/secret=valor. Não tenta ser um DLP
 * completo; é a última barreira para não vazar dado óbvio em texto.
 */
public final class PiiMasker {

    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");
    private static final Pattern SECRET = Pattern.compile(
            "(?i)\\b(password|senha|token|authorization|secret)\\b\\s*[=:]\\s*([^\\s,;]+)");

    private PiiMasker() {
    }

    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String masked = CPF.matcher(value).replaceAll("***.***.***-**");
        masked = EMAIL.matcher(masked).replaceAll("***@***");
        masked = JWT.matcher(masked).replaceAll("***JWT***");
        masked = SECRET.matcher(masked).replaceAll("$1=***");
        return masked;
    }

    public static Object maskArg(Object arg) {
        return arg instanceof String text ? mask(text) : arg;
    }
}
