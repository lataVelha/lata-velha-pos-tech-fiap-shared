package br.com.lata.velha.shared.infrastructure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LevelColorStartConverter extends ClassicConverter {

    private static final String ESC = "\u001B";

    @Override
    public String convert(ILoggingEvent event) {
        return switch (event.getLevel().toInt()) {
            case Level.ERROR_INT -> ESC + "[31m";
            case Level.WARN_INT -> ESC + "[33m";
            case Level.DEBUG_INT -> ESC + "[38;5;135m";
            case Level.TRACE_INT -> ESC + "[38;5;208m";
            default -> "";
        };
    }
}
