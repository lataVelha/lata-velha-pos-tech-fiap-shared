package br.com.lata.velha.shared.domain.events;

import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventsContractTest {

    private static final String PECA_ID = "6915f3a1c2b4d5e6f7a8b9c0";

    private final EventSerializer serializer = new EventSerializer();

    @Test
    @DisplayName("CobrancaEmitidaEvent deve expor os dados e o tipo do evento")
    void deveExporCobrancaEmitida() {
        var event = new CobrancaEmitidaEvent(7L);

        assertThat(event.eventType()).isEqualTo("CobrancaEmitida");
        assertThat(event.getOsId()).isEqualTo(7L);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("OSEndedEvent deve expor os dados e o tipo do evento")
    void deveExporOSEnded() {
        var event = new OSEndedEvent(6L);

        assertThat(event.eventType()).isEqualTo("OSEnded");
        assertThat(event.getOsId()).isEqualTo(6L);
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    @DisplayName("PaymentDoneEvent deve expor os dados e o tipo do evento")
    void deveExporPaymentDone() {
        var event = new PaymentDoneEvent(8L);

        assertThat(event.eventType()).isEqualTo("PaymentDone");
        assertThat(event.getOsId()).isEqualTo(8L);
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    @DisplayName("PecaCompletamenteAlocadaEvent deve expor os dados e o tipo do evento")
    void deveExporPecaCompletamenteAlocada() {
        var event = new PecaCompletamenteAlocadaEvent(10L);

        assertThat(event.eventType()).isEqualTo("PecaCompletamenteAlocada");
        assertThat(event.getExecucaoServicoId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("DeducaoEstoqueFalhouEvent deve expor os dados e o tipo do evento")
    void deveExporDeducaoEstoqueFalhou() {
        var event = new DeducaoEstoqueFalhouEvent(1L, 10L, "estoque insuficiente");

        assertThat(event.eventType()).isEqualTo("DeducaoEstoqueFalhou");
        assertThat(event.getOsId()).isEqualTo(1L);
        assertThat(event.getExecucaoServicoId()).isEqualTo(10L);
        assertThat(event.getMotivo()).isEqualTo("estoque insuficiente");
    }

    @Test
    @DisplayName("InventoryCheckedEvent deve expor os dados e o tipo do evento")
    void deveExporInventoryChecked() {
        var item = new InventoryCheckedEvent.ItemDisponibilidade(10L, true);
        var event = new InventoryCheckedEvent(1L, List.of(item));

        assertThat(event.eventType()).isEqualTo("InventoryChecked");
        assertThat(event.getOsId()).isEqualTo(1L);
        assertThat(event.getItens()).containsExactly(item);
        assertThat(item.execucaoServicoId()).isEqualTo(10L);
        assertThat(item.pecasDisponiveis()).isTrue();
    }

    @Test
    @DisplayName("DiagnosticoFinalizadoEvent deve expor os dados e identificar a peca por String")
    void deveExporDiagnosticoFinalizado() {
        var peca = new DiagnosticoFinalizadoEvent.ItemPeca(PECA_ID, 2);
        var item = new DiagnosticoFinalizadoEvent.ItemDiagnostico(1L, 2L, new BigDecimal("10.00"), List.of(peca));
        var event = new DiagnosticoFinalizadoEvent(5L, List.of(item));

        assertThat(event.eventType()).isEqualTo("DiagnosticoFinalizado");
        assertThat(event.getOsId()).isEqualTo(5L);
        assertThat(event.getItens()).containsExactly(item);
        assertThat(item.execucaoServicoId()).isEqualTo(1L);
        assertThat(item.servicoId()).isEqualTo(2L);
        assertThat(item.valorMaoDeObra()).isEqualByComparingTo("10.00");
        assertThat(item.pecas()).containsExactly(peca);
        assertThat(peca.pecaId()).isEqualTo(PECA_ID);
        assertThat(peca.quantidade()).isEqualTo(2);
    }

    @Test
    @DisplayName("ServiceEndedEvent deve expor os dados e identificar a peca por String")
    void deveExporServiceEnded() {
        var item = new ServiceEndedEvent.ItemPeca(PECA_ID, 2);
        var event = new ServiceEndedEvent(1L, 10L, List.of(item));

        assertThat(event.eventType()).isEqualTo("ServiceEnded");
        assertThat(event.getOsId()).isEqualTo(1L);
        assertThat(event.getExecucaoServicoId()).isEqualTo(10L);
        assertThat(event.getItens()).containsExactly(item);
        assertThat(item.pecaId()).isEqualTo(PECA_ID);
        assertThat(item.quantidade()).isEqualTo(2);
    }

    @Test
    @DisplayName("OrcamentoReprovadoEvent deve expor os dados e o tipo do evento")
    void deveExporOrcamentoReprovado() {
        var event = new OrcamentoReprovadoEvent(42L);

        assertThat(event.eventType()).isEqualTo("OrcamentoReprovado");
        assertThat(event.getOsId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("OrcamentoAprovadoEvent deve copiar itens e tolerar lista nula")
    void deveExporOrcamentoAprovado() {
        var item = new OrcamentoAprovadoEvent.Item(10L, PECA_ID, 2);
        var event = new OrcamentoAprovadoEvent(1L, new BigDecimal("250.00"), List.of(item));

        assertThat(event.eventType()).isEqualTo("OrcamentoAprovado");
        assertThat(event.getOsId()).isEqualTo(1L);
        assertThat(event.getValorTotal()).isEqualByComparingTo("250.00");
        assertThat(event.getItens()).containsExactly(item);
        assertThat(item.execucaoServicoId()).isEqualTo(10L);
        assertThat(item.pecaId()).isEqualTo(PECA_ID);
        assertThat(item.quantidade()).isEqualTo(2);
        assertThat(new OrcamentoAprovadoEvent(1L, BigDecimal.TEN, null).getItens()).isEmpty();
    }

    @Test
    @DisplayName("OrcamentoReprovado faz roundtrip pelo serializer sem perda")
    void deveFazerRoundtripDeOrcamentoReprovado() {
        String json = serializer.serialize(new OrcamentoReprovadoEvent(7L));

        assertThat(json)
                .contains("\"eventType\":\"OrcamentoReprovado\"")
                .contains("\"osId\":7");

        var event = serializer.deserializePayload(serializer.deserializeEnvelope(json), OrcamentoReprovadoEvent.class);

        assertThat(event.getOsId()).isEqualTo(7L);
        assertThat(event.eventType()).isEqualTo("OrcamentoReprovado");
    }

    @Test
    @DisplayName("OrcamentoAprovado serializa pecaId como String e faz roundtrip")
    void deveFazerRoundtripDeOrcamentoAprovado() {
        var event = new OrcamentoAprovadoEvent(42L, new BigDecimal("250.00"),
                List.of(new OrcamentoAprovadoEvent.Item(10L, PECA_ID, 2)));

        String json = serializer.serialize(event);

        assertThat(json)
                .contains("\"eventType\":\"OrcamentoAprovado\"")
                .contains("\"osId\":42")
                .contains("\"valorTotal\":")
                .contains("\"pecaId\":\"" + PECA_ID + "\"");

        var roundtrip = serializer.deserializePayload(serializer.deserializeEnvelope(json), OrcamentoAprovadoEvent.class);

        assertThat(roundtrip.getValorTotal()).isEqualByComparingTo("250.00");
        assertThat(roundtrip.getItens()).hasSize(1);
        assertThat(roundtrip.getItens().getFirst().pecaId()).isEqualTo(PECA_ID);
    }

    @Test
    @DisplayName("ServiceEnded serializa pecaId como String e faz roundtrip")
    void deveFazerRoundtripDeServiceEnded() {
        var event = new ServiceEndedEvent(42L, 10L, List.of(new ServiceEndedEvent.ItemPeca(PECA_ID, 2)));

        String json = serializer.serialize(event);

        assertThat(json)
                .contains("\"eventType\":\"ServiceEnded\"")
                .contains("\"pecaId\":\"" + PECA_ID + "\"");

        var roundtrip = serializer.deserializePayload(serializer.deserializeEnvelope(json), ServiceEndedEvent.class);

        assertThat(roundtrip.getItens()).hasSize(1);
        assertThat(roundtrip.getItens().getFirst().pecaId()).isEqualTo(PECA_ID);
    }

    @Test
    @DisplayName("OrcamentoAprovado sem itens resulta em lista vazia, sem NullPointerException")
    void deveTolerarPayloadSemItens() {
        var json = """
                {
                  "eventId": "%s",
                  "eventType": "OrcamentoAprovado",
                  "occurredAt": "%s",
                  "payload": { "osId": 42, "valorTotal": 250.00 }
                }
                """.formatted(UUID.randomUUID(), Instant.now());

        var event = serializer.deserializePayload(serializer.deserializeEnvelope(json), OrcamentoAprovadoEvent.class);

        assertThat(event.getItens()).isEmpty();
    }

    @Test
    @DisplayName("pecaId numerico de mensagens antigas ainda desserializa como String")
    void deveTolerarPecaIdNumericoLegado() {
        var json = """
                {
                  "eventId": "%s",
                  "eventType": "OrcamentoAprovado",
                  "occurredAt": "%s",
                  "payload": {
                    "osId": 42,
                    "valorTotal": 250.00,
                    "itens": [ { "execucaoServicoId": 10, "pecaId": 100, "quantidade": 2 } ]
                  }
                }
                """.formatted(UUID.randomUUID(), Instant.now());

        var event = serializer.deserializePayload(serializer.deserializeEnvelope(json), OrcamentoAprovadoEvent.class);

        assertThat(event.getItens()).hasSize(1);
        assertThat(event.getItens().getFirst().pecaId()).isEqualTo("100");
    }
}
