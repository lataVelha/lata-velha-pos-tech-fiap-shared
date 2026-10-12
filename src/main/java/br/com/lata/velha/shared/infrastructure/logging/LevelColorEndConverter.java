package br.com.lata.velha.shared.infrastructure.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LevelColorEndConverter extends ClassicConverter {

    private static final String ESC = "\u001B";
    private static final String RESET = ESC + "[0;39m";

    @Override
    public String convert(ILoggingEvent event) {
        return RESET;
    }
}
