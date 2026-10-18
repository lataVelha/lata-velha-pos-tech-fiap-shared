package br.com.lata.velha.shared.infrastructure.messaging;

import br.com.lata.velha.shared.domain.events.AbstractDomainEvent;

public class TestEvent extends AbstractDomainEvent {

    private Long osId;
    private String descricao;

    public TestEvent() {
        super();
    }

    public TestEvent(Long osId, String descricao) {
        super();
        this.osId = osId;
        this.descricao = descricao;
    }

    @Override
    public String eventType() {
        return "TesteEvento";
    }

    public Long getOsId() {
        return osId;
    }

    public void setOsId(Long osId) {
        this.osId = osId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
