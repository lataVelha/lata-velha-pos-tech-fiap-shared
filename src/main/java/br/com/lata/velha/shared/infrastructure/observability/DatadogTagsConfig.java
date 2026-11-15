package br.com.lata.velha.shared.infrastructure.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "otel.exporter.otlp.enabled", havingValue = "true", matchIfMissing = true)
public class DatadogTagsConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatadogTagsConfig.class);

    @Value("${spring.application.name:lata-velha}")
    private String serviceName;

    @Value("${otel.resource.attributes.deployment.environment:local}")
    private String environment;

    @Value("${otel.resource.attributes.service.version:0.0.1}")
    private String version;

    public DatadogTagsConfig() {
        logger.info("DatadogTagsConfig inicializado - Tags padrão do Datadog ativas");
    }

    @Bean
    public SpanProcessor datadogTagsProcessor() {
        logger.info("Registrando SpanProcessor para tags padrão do Datadog - service={}, env={}, version={}", 
            serviceName, environment, version);
        return new SpanProcessor() {
            @Override
            public void onStart(Context parentContext, ReadWriteSpan span) {
                span.setAttribute(AttributeKey.stringKey("service"), serviceName);
                span.setAttribute(AttributeKey.stringKey("env"), environment);
                span.setAttribute(AttributeKey.stringKey("version"), version);
                span.setAttribute(AttributeKey.stringKey("component"), "lata-velha-backend");
                span.setAttribute(AttributeKey.stringKey("language"), "java");
                span.setAttribute(AttributeKey.stringKey("runtime"), "jvm");
                logger.debug("Tags Datadog adicionadas ao span: {}", span.getName());
            }

            @Override
            public boolean isStartRequired() {
                return true;
            }

            @Override
            public void onEnd(io.opentelemetry.sdk.trace.ReadableSpan span) {
            }

            @Override
            public boolean isEndRequired() {
                return false;
            }
        };
    }

    /**
     * Story 19.4/21.2: injeta trace_id/span_id no MDC para que o logback
     * (LogstashEncoder) publique os campos no JSON e o Datadog consiga
     * correlacionar log <-> trace via @trace_id.
     */
    @Bean
    public SpanProcessor mdcTraceContextProcessor() {
        logger.info("Registrando SpanProcessor de MDC - trace_id/span_id nos logs");
        return new SpanProcessor() {
            @Override
            public void onStart(Context parentContext, ReadWriteSpan span) {
                var spanContext = span.getSpanContext();
                if (spanContext.isValid()) {
                    MDC.put("trace_id", spanContext.getTraceId());
                    MDC.put("span_id", spanContext.getSpanId());
                }
            }

            @Override
            public void onEnd(ReadableSpan span) {
                MDC.remove("trace_id");
                MDC.remove("span_id");
            }

            @Override
            public boolean isStartRequired() {
                return true;
            }

            @Override
            public boolean isEndRequired() {
                return true;
            }
        };
    }
}
