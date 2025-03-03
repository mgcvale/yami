package com.yamiapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;

public class Yami {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(dotenvEntry -> System.setProperty(dotenvEntry.getKey(), dotenvEntry.getValue()));

        SpringApplication.run(SpringApp.class, args);
    }
}
