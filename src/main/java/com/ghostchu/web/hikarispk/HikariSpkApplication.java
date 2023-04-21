package com.ghostchu.web.hikarispk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@SpringBootApplication
public class HikariSpkApplication {

    public static void main(String[] args) {
        File file = new File("application.yml");
        if (!file.exists()) {
            try {
                Files.copy(HikariSpkApplication.class.getResourceAsStream("/application.yml"), file.toPath());
            } catch (IOException e) {
                System.err.println("Cannot copy application.yml from jar to " + file.getAbsolutePath());
            }
        }
        SpringApplication.run(HikariSpkApplication.class, args);
    }

}
