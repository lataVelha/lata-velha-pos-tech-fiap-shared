package br.com.lata.velha.shared.application.logging;

public interface Logger {
    void logInfo(String message);
    void logInfo(String format, Object... args);

    void logWarn(String message);
    void logWarn(String format, Object... args);

    void logDebug(String message);
    void logDebug(String format, Object... args);

    void logTrace(String message);
    void logTrace(String format, Object... args);

    void logError(String message);
    void logError(String format, Object... args);
    void logError(String message, Exception ex);
}
