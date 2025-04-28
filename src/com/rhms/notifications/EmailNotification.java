package com.rhms.notifications;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

// Handles email notifications using Gmail SMTP server
public class EmailNotification implements Notifiable {
    // Gmail credentials for sending emails
    private final String senderEmail = "momin332472@gmail.com"; // My Gmail
    private final String senderPassword = "evhe zjge jpsm hiwb"; // My Gmail App Password (through security in google account)

    // Sends an email notification to the specified recipient
    @Override
    public void sendNotification(String recipient, String subject, String message) {
        // Configure Gmail SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Create authenticated email session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // Create and configure email message
            Message mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(senderEmail));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(message);

            // Send email and log success
            Transport.send(mimeMessage);
            System.out.println("Email sent successfully to: " + recipient);
        } catch (MessagingException e) {
            // Log any errors during email sending
            System.out.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}