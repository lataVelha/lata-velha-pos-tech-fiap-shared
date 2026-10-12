package br.com.lata.velha.shared.infrastructure.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "otel.exporter.otlp.enabled", havingValue = "true", matchIfMissing = true)
public class OpenTelemetryWebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryWebConfig.class);

    public OpenTelemetryWebConfig() {
        logger.info("OpenTelemetryWebConfig inicializado - Interceptor HTTP para Datadog ativo");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        logger.info("Registrando OpenTelemetryInterceptor para enriquecimento de spans HTTP");
        registry.addInterceptor(new OpenTelemetryInterceptor());
    }

    private static class OpenTelemetryInterceptor implements HandlerInterceptor {

        private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryInterceptor.class);

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            Span span = Span.current();
            if (span == null || !span.getSpanContext().isValid()) {
                logger.debug("Span inválido ou nulo para requisição: {} {}", request.getMethod(), request.getRequestURI());
                return true;
            }

            String method = request.getMethod();
            String path = request.getRequestURI();
            String pattern = getHandlerPattern(request);

            span.setAttribute("http.method", method);
            span.setAttribute("http.request.method", method);
            span.setAttribute("http.url", request.getRequestURL().toString());
            span.setAttribute("url.full", request.getRequestURL().toString());
            span.setAttribute("http.target", path);
            span.setAttribute("url.path", path);
            span.setAttribute("http.host", request.getServerName());
            span.setAttribute("server.address", request.getServerName());
            span.setAttribute("http.scheme", request.getScheme());
            span.setAttribute("url.scheme", request.getScheme());
            span.setAttribute("http.user_agent", request.getHeader("User-Agent"));
            span.setAttribute("user_agent.original", request.getHeader("User-Agent"));
            span.setAttribute("http.client_ip", getClientIp(request));
            span.setAttribute("client.address", getClientIp(request));

            if (pattern != null && !pattern.isEmpty()) {
                span.setAttribute("http.route", pattern);
                span.setAttribute("resource.name", method + " " + pattern);
                span.setAttribute("operation.name", "http.server.request");
                logger.debug("Span HTTP enriquecido: {} {} - route: {}", method, path, pattern);
            } else {
                span.setAttribute("resource.name", method + " " + path);
                span.setAttribute("operation.name", "http.server.request");
                logger.debug("Span HTTP enriquecido: {} {} - sem pattern", method, path);
            }

            span.setAttribute("span.kind", SpanKind.SERVER.name());

            return true;
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response,
                               Object handler, ModelAndView modelAndView) {
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            Span span = Span.current();
            if (span == null || !span.getSpanContext().isValid()) {
                return;
            }

            int statusCode = response.getStatus();
            span.setAttribute("http.status_code", statusCode);
            span.setAttribute("http.response.status_code", statusCode);

            if (statusCode >= 500) {
                span.setStatus(StatusCode.ERROR, "HTTP " + statusCode);
                logger.debug("Span HTTP finalizado com erro: {} {} - status: {}", 
                    request.getMethod(), request.getRequestURI(), statusCode);
            } else if (statusCode >= 400) {
                span.setStatus(StatusCode.ERROR, "HTTP " + statusCode);
                logger.debug("Span HTTP finalizado com erro cliente: {} {} - status: {}", 
                    request.getMethod(), request.getRequestURI(), statusCode);
            } else {
                span.setStatus(StatusCode.OK);
                logger.debug("Span HTTP finalizado com sucesso: {} {} - status: {}", 
                    request.getMethod(), request.getRequestURI(), statusCode);
            }

            if (ex != null) {
                span.setStatus(StatusCode.ERROR, ex.getMessage());
                span.recordException(ex);
                logger.error("Exceção registrada no span: {} {} - {}", 
                    request.getMethod(), request.getRequestURI(), ex.getMessage());
            }
        }

        private String getHandlerPattern(HttpServletRequest request) {
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            return pattern != null ? pattern.toString() : null;
        }

        private String getClientIp(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            return request.getRemoteAddr();
        }
    }
}
