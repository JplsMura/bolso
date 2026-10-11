/**
 * Módulo Lançamentos: lançamento, tag, preset e parcelamento.
 *
 * <p>Estrutura (hexagonal): {@code domain} (Java puro), {@code application} (casos de uso e portas),
 * {@code adapter.in.web} e {@code adapter.out.persistence}. Os outros módulos só enxergam a
 * {@code LancamentosApi}, os tipos que ela devolve e a porta {@code FaturasDoCartao}, que o módulo Cartões
 * implementa (assim a dependência entre os dois fica num sentido só).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Lançamentos")
package dev.bolso.lancamentos;
