package br.com.lata.velha.shared.infrastructure.messaging;

import br.com.lata.velha.shared.application.gateways.EventHandler;
import br.com.lata.velha.shared.application.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class EventHandlerRegistryTest {

    @Mock
    private Logger logger;

    private final EventSerializer serializer = new EventSerializer();

    @Test
    void deveDespacharParaOHandlerRegistradoComOEventTypeCorrespondente() {
        var captured = new java.util.concurrent.atomic.AtomicReference<TestEvent>();
        EventHandler<TestEvent> handler = new EventHandler<>() {
            @Override
            public String eventType() {
                return "TesteEvento";
            }

            @Override
            public Class<TestEvent> eventClass() {
                return TestEvent.class;
            }

            @Override
            public void handle(TestEvent event) {
                captured.set(event);
            }
        };
        var registry = new EventHandlerRegistry(List.of(handler), serializer, logger);
        var event = new TestEvent(42L, "evento de teste");
        var envelope = serializer.deserializeEnvelope(serializer.serialize(event));

        registry.dispatch(envelope);

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getOsId()).isEqualTo(42L);
    }

    @Test
    void naoDeveLancarQuandoNenhumHandlerEstaRegistradoParaOEventType() {
        var registry = new EventHandlerRegistry(List.of(), serializer, logger);
        var envelope = serializer.deserializeEnvelope(serializer.serialize(new TestEvent(1L, "x")));

        assertThatNoException().isThrownBy(() -> registry.dispatch(envelope));
    }
}
