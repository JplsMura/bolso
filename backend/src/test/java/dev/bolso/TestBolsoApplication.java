package dev.bolso;

import org.springframework.boot.SpringApplication;

/** Roda a API localmente com um PostgreSQL em contêiner: ./mvnw spring-boot:test-run */
public class TestBolsoApplication {

    public static void main(String[] args) {
        SpringApplication.from(BolsoApplication::main).with(TestcontainersConfig.class).run(args);
    }
}
