package dev.bolso.lancamentos.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Espelho de {@code finance.transaction} para o JPA (Hibernate em {@code validate}: precisa bater com o
 * Flyway). Fica só no adaptador; o domínio é {@code Lancamento}. Enums viram texto aqui, no mapeamento.
 * As tags ficam em {@code finance.transaction_tag}, gravadas à parte pelo repositório.
 */
@Entity
@Table(name = "transaction", schema = "finance")
class LancamentoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    UUID espacoId;

    @Column(name = "direction", nullable = false)
    String direcao;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    BigDecimal valor;

    @Column(name = "description", nullable = false)
    String descricao;

    @Column(name = "occurred_on", nullable = false)
    LocalDate data;

    @Column(name = "reference_month", nullable = false)
    LocalDate mesReferencia;

    @Column(name = "goal_id")
    UUID metaId;

    @Column(name = "payment_method")
    String formaPagamento;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant criadoEm;

    @Column(name = "updated_at", nullable = false)
    Instant atualizadoEm;

    @Column(name = "deleted_at")
    Instant excluidoEm;

    @Version
    @Column(name = "version", nullable = false)
    Long versao;

    protected LancamentoEntity() {}
}
