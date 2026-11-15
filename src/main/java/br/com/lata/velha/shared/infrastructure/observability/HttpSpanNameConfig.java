package br.com.lata.velha.shared.infrastructure.observability;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Story 19.3: padroniza o nome da operação do span HTTP server para
 * {@code http.request}. As trace metrics passam a ser
 * {@code trace.http.request.*}, alinhadas ao contrato do gate DT-DD.
 */
@Configuration
@ConditionalOnProperty(name = "otel.exporter.otlp.enabled", havingValue = "true", matchIfMissing = true)
public class HttpSpanNameConfig {

    @Bean
    public SpanProcessor httpSpanNameProcessor() {
        return new SpanProcessor() {
            @Override
            public void onStart(Context parentContext, ReadWriteSpan span) {
                if (span.getKind() == SpanKind.SERVER) {
                    span.updateName("http.request");
                }
            }

            @Override
            public void onEnd(ReadableSpan span) {
                // nada
            }

            @Override
            public boolean isStartRequired() {
                return true;
            }

            @Override
            public boolean isEndRequired() {
                return false;
            }
        };
    }
}
