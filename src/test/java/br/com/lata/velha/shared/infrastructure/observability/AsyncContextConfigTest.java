package br.com.lata.velha.shared.infrastructure.observability;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncContextConfigTest {

    @Test
    void propagaContextoOtelEMdcParaThreadDoExecutor() throws Exception {
        ContextKey<String> key = ContextKey.named("test-key");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setTaskDecorator(new AsyncContextConfig().otelContextTaskDecorator());
        executor.initialize();

        try (Scope ignored = Context.root().with(key, "parent").makeCurrent()) {
            MDC.put("trace_id", "abc123");
            Future<String> future = executor.submit(
                    () -> Context.current().get(key) + ":" + MDC.get("trace_id"));
            assertEquals("parent:abc123", future.get(5, TimeUnit.SECONDS));
        } finally {
            MDC.clear();
            executor.shutdown();
        }
    }
}
