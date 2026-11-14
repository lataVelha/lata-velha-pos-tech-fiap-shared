package br.com.lata.velha.shared.domain.events;

public final class PecaCompletamenteAlocadaEvent extends AbstractDomainEvent {

    private final Long execucaoServicoId;

    public PecaCompletamenteAlocadaEvent(Long execucaoServicoId) {
        this.execucaoServicoId = execucaoServicoId;
    }

    @Override
    public String eventType() {
        return "PecaCompletamenteAlocada";
    }

    public Long getExecucaoServicoId() {
        return execucaoServicoId;
    }
}
