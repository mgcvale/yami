package com.yamiapp;

import com.yamiapp.config.DotenvConfig;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import java.util.logging.Level;
import java.util.logging.Logger;

@ConfigurationPropertiesScan
@SpringBootApplication
public class Yami {
    public static void main(String[] args) {
        ConfigurableEnvironment environment = new StandardEnvironment();

        Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMissing()
            .load();

        dotenv.entries().forEach(e ->
            System.setProperty(e.getKey(), e.getValue())
        );


        String production = System.getenv("PRODUCTION");
        if ("true".equalsIgnoreCase(production)) {
            Logger.getAnonymousLogger().log(Level.INFO, "Running Yami in production mode");
            environment.setActiveProfiles("prod");
        }

        SpringApplication app = new SpringApplication(Yami.class);
        app.setEnvironment(environment);
        app.run(args);
    }
}