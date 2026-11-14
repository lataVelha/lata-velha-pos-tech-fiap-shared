package br.com.lata.velha.shared.domain.events;

import java.util.List;

public final class InventoryCheckedEvent extends AbstractDomainEvent {

    private final Long osId;
    private final List<ItemDisponibilidade> itens;

    public InventoryCheckedEvent(Long osId, List<ItemDisponibilidade> itens) {
        this.osId = osId;
        this.itens = itens;
    }

    @Override
    public String eventType() {
        return "InventoryChecked";
    }

    public Long getOsId() {
        return osId;
    }

    public List<ItemDisponibilidade> getItens() {
        return itens;
    }

    public record ItemDisponibilidade(Long execucaoServicoId, Boolean pecasDisponiveis) {
    }
}
