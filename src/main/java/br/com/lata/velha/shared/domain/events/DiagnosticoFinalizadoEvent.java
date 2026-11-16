package br.com.lata.velha.shared.domain.events;

import java.math.BigDecimal;
import java.util.List;

public final class DiagnosticoFinalizadoEvent extends AbstractDomainEvent {

    private final Long osId;
    private final List<ItemDiagnostico> itens;
    private final String emailProprietario;

    public DiagnosticoFinalizadoEvent(Long osId, List<ItemDiagnostico> itens, String emailProprietario) {
        this.osId = osId;
        this.itens = itens;
        this.emailProprietario = emailProprietario;
    }

    @Override
    public String eventType() {
        return "DiagnosticoFinalizado";
    }

    public Long getOsId() {
        return osId;
    }

    public List<ItemDiagnostico> getItens() {
        return itens;
    }

    public String getEmailProprietario() {
        return emailProprietario;
    }

    public record ItemDiagnostico(Long execucaoServicoId, Long servicoId, BigDecimal valorMaoDeObra, List<ItemPeca> pecas) {
    }

    public record ItemPeca(String pecaId, Integer quantidade) {
    }
}
