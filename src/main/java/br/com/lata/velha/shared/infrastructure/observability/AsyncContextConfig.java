package br.com.lata.velha.shared.infrastructure.observability;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Story 19.4: propaga o contexto OTel e o MDC para tarefas executadas em
 * {@code ThreadPoolTaskExecutor}. Basta o executor usar este decorator
 * (o auto-configurado do Spring Boot aproveita o bean quando presente).
 */
@Configuration
public class AsyncContextConfig {

    @Bean
    public TaskDecorator otelContextTaskDecorator() {
        return runnable -> {
            Context context = Context.current();
            Map<String, String> mdc = MDC.getCopyOfContextMap();
            return () -> {
                try (Scope ignored = context.makeCurrent()) {
                    if (mdc != null) {
                        MDC.setContextMap(mdc);
                    }
                    try {
                        runnable.run();
                    } finally {
                        MDC.clear();
                    }
                }
            };
        };
    }
}
