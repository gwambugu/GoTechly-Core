package com.dotechly.gotechly.core.service.partner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends partner notification emails asynchronously.
 * On approval, the partner receives their credentials: salt, agentId, clientShortName.
 */
@Service
public class PartnerEmailService {

    private static final Logger log = LoggerFactory.getLogger(PartnerEmailService.class);

    private final JavaMailSender mailSender;

    public PartnerEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends partner onboarding credentials after approval.
     *
     * @param recipientEmail the partner's contact email
     * @param clientShortName the partner's short name identifier
     * @param agentId the partner's agent ID
     * @param salt the unencrypted salt value
     */
    @Async("auditExecutor")
    public void sendApprovalNotification(String recipientEmail, String clientShortName, String agentId, String salt) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("GoTechly Partner Onboarding - Credentials");
            message.setText(
                "Dear Partner,\n\n" +
                "Your partner registration has been approved. Below are your credentials for integration:\n\n" +
                "Client Short Name: " + clientShortName + "\n" +
                "Agent ID: " + agentId + "\n" +
                "Salt: " + salt + "\n\n" +
                "Please store these credentials securely. The salt is used for request signing " +
                "and will not be shared again.\n\n" +
                "For integration support, please contact the GoTechly technical team.\n\n" +
                "Regards,\n" +
                "GoTechly Payment Gateway"
            );
            mailSender.send(message);
            log.info("Approval notification sent to partner '{}' at {}", clientShortName, recipientEmail);
        } catch (MailException e) {
            log.error("Failed to send approval email to {} for partner '{}': {}", recipientEmail, clientShortName, e.getMessage());
        }
    }
}
