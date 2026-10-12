package br.com.lata.velha.shared.domain.exceptions;

public class ResourceAlreadyExistsException extends DomainException {
    public ResourceAlreadyExistsException(String message) { super(message); }
}