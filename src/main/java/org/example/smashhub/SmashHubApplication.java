package org.example.smashhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SmashHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmashHubApplication.class, args);
    }

}
