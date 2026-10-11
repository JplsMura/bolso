/**
 * Módulo Lançamentos: lançamento, tag, preset e parcelamento.
 *
 * <p>Estrutura (hexagonal): {@code domain} (Java puro), {@code application} (casos de uso e portas),
 * {@code adapter.in.web} e {@code adapter.out.persistence}. Os outros módulos só enxergam a
 * {@code LancamentosApi} na raiz deste pacote, criada pela primeira feature que precisar dela.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Lançamentos")
package dev.bolso.lancamentos;
