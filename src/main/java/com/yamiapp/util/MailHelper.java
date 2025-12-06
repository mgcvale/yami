package com.yamiapp.util;

import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.InternalServerException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class MailHelper {

    private final Session session;
    private final String username;

    public MailHelper(Session session) {
        this.session = session;

        this.username = session.getProperty("mail.smtp.user");

        if (username == null || username.isBlank()) {
            throw new IllegalStateException(
                "mail.smtp.user property is missing. Make sure you set it in EmailConfig."
            ); // this should never throw
        }
    }
    public void sendMail(String to, String subject, String content) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);

        } catch (MessagingException e) {
            throw new InternalServerException(ErrorStrings.MAIL_ERROR.getMessage());
        }
    }

    public void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);

        } catch (MessagingException e) {
            throw new InternalServerException(ErrorStrings.MAIL_ERROR.getMessage());
        }
    }
}