package br.com.lata.velha.shared.domain.events;

public final class CobrancaEmitidaEvent extends AbstractDomainEvent {

    private final Long osId;

    public CobrancaEmitidaEvent(Long osId) {
        this.osId = osId;
    }

    @Override
    public String eventType() {
        return "CobrancaEmitida";
    }

    public Long getOsId() {
        return osId;
    }
}
