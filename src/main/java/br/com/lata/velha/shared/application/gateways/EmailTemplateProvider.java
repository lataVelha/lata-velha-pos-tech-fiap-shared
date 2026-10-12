package br.com.lata.velha.shared.application.gateways;

import java.util.Map;

public interface EmailTemplateProvider {

    String render(String templateName, Map<String, Object> variables);
}
