package com.example.datn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DatnApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(DatnApplication.class, args);
    }

    private static void loadDotEnv() {
        java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
        if (!java.nio.file.Files.exists(envPath)) {
            envPath = java.nio.file.Paths.get("BE", ".env");
        }
        if (java.nio.file.Files.exists(envPath)) {
            try {
                for (String line : java.nio.file.Files.readAllLines(envPath)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                        continue;
                    }
                    int eqIdx = trimmed.indexOf('=');
                    String key = trimmed.substring(0, eqIdx).trim();
                    String val = trimmed.substring(eqIdx + 1).trim();
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, val);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

}
