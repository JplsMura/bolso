package dev.bolso;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BolsoApplication {

    public static void main(String[] args) {
        // tudo em UTC por dentro; America/Sao_Paulo só na borda (API e tela)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(BolsoApplication.class, args);
    }
}
