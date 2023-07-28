package ru.crptAPI.crptAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;


@SpringBootApplication
public class CrptApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrptApiApplication.class, args);
    }

    @Bean
    public Controller.Limiter limiter() {
        return new Controller.Limiter(TimeUnit.MINUTES, 2);
    }

}
