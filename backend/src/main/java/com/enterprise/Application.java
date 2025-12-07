package com.enterprise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.enterprise.model")
@EnableJpaRepositories("com.enterprise.repository")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
