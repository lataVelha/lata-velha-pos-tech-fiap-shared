package br.com.lata.velha.shared.infrastructure.logging;

import br.com.lata.velha.shared.application.logging.Logger;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LocationAwareLogger;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Slf4jLogger implements Logger {

    private static final String FQCN = Slf4jLogger.class.getName();

    @Override
    public void logInfo(String message) {
        log(LocationAwareLogger.INFO_INT, message, null, null);
    }

    @Override
    public void logInfo(String format, Object... args) {
        log(LocationAwareLogger.INFO_INT, format, args, null);
    }

    @Override
    public void logWarn(String message) {
        log(LocationAwareLogger.WARN_INT, message, null, null);
    }

    @Override
    public void logWarn(String format, Object... args) {
        log(LocationAwareLogger.WARN_INT, format, args, null);
    }

    @Override
    public void logDebug(String message) {
        log(LocationAwareLogger.DEBUG_INT, message, null, null);
    }

    @Override
    public void logDebug(String format, Object... args) {
        log(LocationAwareLogger.DEBUG_INT, format, args, null);
    }

    @Override
    public void logTrace(String message) {
        log(LocationAwareLogger.TRACE_INT, message, null, null);
    }

    @Override
    public void logTrace(String format, Object... args) {
        log(LocationAwareLogger.TRACE_INT, format, args, null);
    }

    @Override
    public void logError(String message) {
        log(LocationAwareLogger.ERROR_INT, message, null, null);
    }

    @Override
    public void logError(String format, Object... args) {
        log(LocationAwareLogger.ERROR_INT, format, args, null);
    }

    @Override
    public void logError(String message, Exception ex) {
        log(LocationAwareLogger.ERROR_INT, message, null, ex);
    }

    private void log(int level, String message, Object[] args, Throwable t) {
        if (log instanceof LocationAwareLogger locationAwareLogger) {
            locationAwareLogger.log(null, FQCN, level, message, args, t);
        } else {
            logFallback(level, message, args, t);
        }
    }

    private void logFallback(int level, String message, Object[] args, Throwable t) {
        Object[] safeArgs = args == null ? new Object[0] : args;
        switch (level) {
            case LocationAwareLogger.ERROR_INT -> {
                if (t != null) {
                    log.error(message, t);
                } else {
                    log.error(message, safeArgs);
                }
            }
            case LocationAwareLogger.WARN_INT -> log.warn(message, safeArgs);
            case LocationAwareLogger.DEBUG_INT -> log.debug(message, safeArgs);
            case LocationAwareLogger.TRACE_INT -> log.trace(message, safeArgs);
            default -> log.info(message, safeArgs);
        }
    }
}
