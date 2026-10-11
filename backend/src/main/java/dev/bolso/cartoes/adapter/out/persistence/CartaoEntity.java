package dev.bolso.cartoes.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Espelho de {@code finance.credit_card} para o JPA (Hibernate em {@code validate}). O domínio é {@code Cartao}. */
@Entity
@Table(name = "credit_card", schema = "finance")
class CartaoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    UUID espacoId;

    @Column(name = "name", nullable = false)
    String nome;

    @Column(name = "closing_day", nullable = false)
    Integer diaFechamento;

    @Column(name = "due_day", nullable = false)
    Integer diaVencimento;

    @Column(name = "credit_limit", nullable = false, precision = 14, scale = 2)
    BigDecimal limite;

    @Column(name = "archived_at")
    Instant arquivadoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant criadoEm;

    @Column(name = "updated_at", nullable = false)
    Instant atualizadoEm;

    @Version
    @Column(name = "version", nullable = false)
    Long versao;

    protected CartaoEntity() {}
}
