package com.rhms.notifications;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

// Handles email notifications using Gmail SMTP server
public class EmailNotification implements Notifiable {
    private static final Logger LOGGER = Logger.getLogger(EmailNotification.class.getName());
    
    // Gmail credentials for sending emails
    private final String senderEmail = "remotehospitalsystem@gmail.com"; // My Gmail
    private final String senderPassword = "vcgs lcil ubcp cept "; // My Gmail App Password

    // Sends an email notification to the specified recipient
    @Override
    public boolean sendNotification(String recipient, String subject, String message) {
        // Basic input validation
        if (recipient == null || recipient.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Cannot send email: Empty recipient");
            return false;
        }
        
        if (message == null || message.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Cannot send email: Empty message");
            return false;
        }
        
        // Use default subject if none provided
        if (subject == null || subject.trim().isEmpty()) {
            subject = "RHMS Notification";
        }

        // Configure Gmail SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.connectiontimeout", "5000"); // 5 second timeout
        props.put("mail.smtp.timeout", "5000");

        try {
            // Create authenticated email session
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, senderPassword);
                }
            });

            // Create and configure email message
            Message mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(senderEmail));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(message);

            // Send email
            Transport.send(mimeMessage);
            LOGGER.log(Level.INFO, "Email sent successfully to: {0}", recipient);
            return true;
            
        } catch (AddressException e) {
            LOGGER.log(Level.SEVERE, "Invalid email address: {0}", e.getMessage());
            return false;
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email: {0}", e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error while sending email: {0}", e.getMessage());
            return false;
        }
    }
}
