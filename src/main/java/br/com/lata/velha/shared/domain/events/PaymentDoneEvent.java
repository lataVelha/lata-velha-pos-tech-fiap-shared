package br.com.lata.velha.shared.domain.events;

public final class PaymentDoneEvent extends AbstractDomainEvent {

    private final Long osId;

    public PaymentDoneEvent(Long osId) {
        this.osId = osId;
    }

    @Override
    public String eventType() {
        return "PaymentDone";
    }

    public Long getOsId() {
        return osId;
    }
}
