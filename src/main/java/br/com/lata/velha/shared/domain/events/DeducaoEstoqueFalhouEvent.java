package br.com.lata.velha.shared.domain.events;

public final class DeducaoEstoqueFalhouEvent extends AbstractDomainEvent {

    private final Long osId;
    private final Long execucaoServicoId;
    private final String motivo;

    public DeducaoEstoqueFalhouEvent(Long osId, Long execucaoServicoId, String motivo) {
        this.osId = osId;
        this.execucaoServicoId = execucaoServicoId;
        this.motivo = motivo;
    }

    @Override
    public String eventType() {
        return "DeducaoEstoqueFalhou";
    }

    public Long getOsId() {
        return osId;
    }

    public Long getExecucaoServicoId() {
        return execucaoServicoId;
    }

    public String getMotivo() {
        return motivo;
    }
}
