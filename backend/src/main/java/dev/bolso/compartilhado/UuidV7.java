package dev.bolso.compartilhado;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID v7 (RFC 9562): 48 bits de milissegundos desde 1970, versão 7, variante 10 e o resto aleatório.
 * Ordena por data de criação, o que mantém os índices do PostgreSQL compactos.
 */
public final class UuidV7 {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private UuidV7() {}

    public static UUID gerar() {
        return gerar(System.currentTimeMillis());
    }

    static UUID gerar(long milissegundos) {
        byte[] sorteio = new byte[10];
        ALEATORIO.nextBytes(sorteio);

        // 48 bits de tempo | 4 bits de versão (7) | 12 bits aleatórios
        long maisSignificativos = (milissegundos & 0xFFFFFFFFFFFFL) << 16;
        maisSignificativos |= 0x7000L | ((sorteio[0] & 0x0FL) << 8) | (sorteio[1] & 0xFFL);

        // 2 bits de variante (10) | 62 bits aleatórios
        long menosSignificativos = 0;
        for (int i = 2; i < 10; i++) {
            menosSignificativos = (menosSignificativos << 8) | (sorteio[i] & 0xFFL);
        }
        menosSignificativos = (menosSignificativos & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(maisSignificativos, menosSignificativos);
    }
}
