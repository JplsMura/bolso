package dev.bolso.cartoes.adapter.out.persistence;

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
 * Espelho de {@code finance.card_invoice} para o JPA, usado só para mudar o status (com trava e {@code @Version}).
 * A criação é por SQL ({@code ON CONFLICT DO NOTHING}), para duas compras simultâneas não criarem a mesma fatura.
 */
@Entity
@Table(name = "card_invoice", schema = "finance")
class FaturaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    UUID espacoId;

    @Column(name = "card_id", nullable = false, updatable = false)
    UUID cartaoId;

    @Column(name = "reference_month", nullable = false, updatable = false)
    LocalDate mes;

    @Column(name = "closing_date", nullable = false, updatable = false)
    LocalDate fechamento;

    @Column(name = "due_date", nullable = false, updatable = false)
    LocalDate vencimento;

    @Column(name = "status", nullable = false)
    String status;

    @Column(name = "total_amount", precision = 14, scale = 2)
    BigDecimal total;

    @Column(name = "paid_on")
    LocalDate pagaEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant criadoEm;

    @Column(name = "updated_at", nullable = false)
    Instant atualizadoEm;

    @Version
    @Column(name = "version", nullable = false)
    Long versao;

    protected FaturaEntity() {}
}
