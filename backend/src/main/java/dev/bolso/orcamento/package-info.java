/**
 * Módulo Orçamento: as 6 metas, percentual por mês e fechamento do mês.
 *
 * <p>Estrutura (hexagonal): {@code domain} (Java puro), {@code application} (casos de uso e portas),
 * {@code adapter.in.web} e {@code adapter.out.persistence}. Os outros módulos só enxergam a
 * {@code OrcamentoApi} na raiz deste pacote, criada pela primeira feature que precisar dela.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Orçamento")
package dev.bolso.orcamento;
