package br.com.lata.velha.shared.api.handlers;

import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.domain.exceptions.DomainException;
import br.com.lata.velha.shared.domain.exceptions.NotFoundException;
import br.com.lata.velha.shared.domain.exceptions.ResourceAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Logger logger;

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        logger.logWarn("Requisição rejeitada - status=404 NOT_FOUND, motivo={}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ResourceAlreadyExistsException ex) {
        logger.logWarn("Requisição rejeitada - status=409 CONFLICT, motivo={}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        logger.logWarn("Requisição rejeitada - status=422 UNPROCESSABLE_ENTITY, motivo={}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.logWarn("Requisição rejeitada - status=400 BAD_REQUEST, motivo={}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var binding = ex.getBindingResult();
        var errors = new java.util.ArrayList<String>();
        binding.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .forEach(errors::add);
        binding.getGlobalErrors().stream()
                .map(e -> e.getDefaultMessage())
                .forEach(errors::add);

        // Do not log the full messages list: field validation messages can echo back the
        // rejected value (e.g. an invalid email/CPF), so only log field names + a count.
        var fieldNames = binding.getFieldErrors().stream().map(e -> e.getField()).distinct().toList();
        logger.logWarn("Requisição rejeitada - status=400 BAD_REQUEST, validationErrorCount={}, campos={}",
                errors.size(), fieldNames);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("error", "Validation Error", "messages", errors, "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomain(DomainException ex) {
        logger.logWarn("Requisição rejeitada - status=400 BAD_REQUEST, motivo={}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        logger.logWarn("Requisição rejeitada - status=404 NOT_FOUND, recurso não mapeado");
        return build(HttpStatus.NOT_FOUND, "Not Found", "Recurso não encontrado: " + ex.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        logger.logError("Erro inesperado não tratado - status=500 INTERNAL_SERVER_ERROR", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Ocorreu um erro interno. Tente novamente mais tarde.");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(
                Map.of("error", error, "message", message, "timestamp", Instant.now().toString()));
    }
}
