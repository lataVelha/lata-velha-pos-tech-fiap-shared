package br.com.lata.velha.shared.domain.events;

public final class OrcamentoReprovadoEvent extends AbstractDomainEvent {

    private final Long osId;

    public OrcamentoReprovadoEvent(Long osId) {
        this.osId = osId;
    }

    @Override
    public String eventType() {
        return "OrcamentoReprovado";
    }

    public Long getOsId() {
        return osId;
    }
}
