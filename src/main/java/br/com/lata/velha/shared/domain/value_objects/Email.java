package br.com.lata.velha.shared.domain.value_objects;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object que encapsula o email do usuário.
 * Responsável por validar o formato do email.
 */
public final class Email {
    private static final int MAX_LENGTH = 254;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private final String valor;

    private Email(String valor) {
        validate(valor);
        this.valor = valor.toLowerCase().trim();
    }

    public static Email fromString(String email) {
        return new Email(email);
    }

    public String getValor() {
        return valor;
    }

    private static void validate(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email não pode ser vazio");
        }

        String trimmed = email.trim();

        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Email deve ter no máximo %d caracteres", MAX_LENGTH));
        }

        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Formato de email inválido");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Email that)) return false;
        return Objects.equals(valor, that.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
