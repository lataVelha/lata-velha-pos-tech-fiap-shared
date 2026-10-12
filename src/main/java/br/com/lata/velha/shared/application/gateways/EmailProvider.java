package br.com.lata.velha.shared.application.gateways;

public interface EmailProvider {

    void send(String to, String subject, String htmlContent);
}
