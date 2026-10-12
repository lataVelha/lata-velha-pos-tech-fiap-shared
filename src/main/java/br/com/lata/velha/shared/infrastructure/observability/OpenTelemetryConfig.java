package br.com.lata.velha.shared.infrastructure.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "otel.exporter.otlp.enabled", havingValue = "true", matchIfMissing = true)
public class OpenTelemetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    public OpenTelemetryConfig() {
        logger.info("OpenTelemetryConfig inicializado - Datadog resource enrichment ativo");
    }

    @Bean
    public SpanProcessor datadogResourceEnrichmentProcessor() {
        logger.info("Registrando SpanProcessor para enriquecimento de resources do Datadog");
        return new SpanProcessor() {
            @Override
            public void onStart(Context parentContext, ReadWriteSpan span) {
                String spanName = span.getName();
                if (spanName != null && !spanName.isEmpty()) {
                    span.setAttribute(AttributeKey.stringKey("resource.name"), spanName);
                    logger.debug("Span iniciado: {} - resource.name definido", spanName);
                }
            }

            @Override
            public boolean isStartRequired() {
                return true;
            }

            @Override
            public void onEnd(ReadableSpan span) {
                logger.debug("Span finalizado: {} - attributes: {}", span.getName(), span.getAttributes().asMap().size());
            }

            @Override
            public boolean isEndRequired() {
                return true;
            }
        };
    }
}
