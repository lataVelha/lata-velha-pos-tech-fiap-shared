package br.com.lata.velha.shared.domain.exceptions;

public class DomainValidationException extends DomainException{
    public DomainValidationException(String message) {
        super(message);
    }
}
