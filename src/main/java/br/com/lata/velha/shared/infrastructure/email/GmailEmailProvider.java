package br.com.lata.velha.shared.infrastructure.email;

import br.com.lata.velha.shared.application.gateways.EmailProvider;
import br.com.lata.velha.shared.application.logging.Logger;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GmailEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;
    private final Logger logger;

    @Async
    @Override
    public void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            logger.logWarn("Falha ao montar/enviar e-mail via Gmail SMTP - assunto='{}', causa={}. Destinatário não receberá esta notificação.",
                    subject, e.getClass().getSimpleName());
            throw new RuntimeException("Falha ao enviar email", e);
        }
    }
}
