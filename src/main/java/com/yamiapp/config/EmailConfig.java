package com.yamiapp.config;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class EmailConfig {

    @Value("${mail.smtp.host}")
    private String host;

    @Value("${mail.smtp.port}")
    private int port;

    @Value("${mail.smtp.user}")
    private String username;

    @Value("${mail.smtp.password}")
    private String password;

    @Value("${mail.smtp.tls}")
    private boolean useTls;

    @Value("${mail.smtp.auth}")
    private boolean auth;

    @Bean
    public Session mailSession() {
        Properties props = new Properties();

        props.put("mail.smtp.user", username);
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.starttls.enable", String.valueOf(useTls));
        props.put("mail.smtp.auth", String.valueOf(auth));

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
}
