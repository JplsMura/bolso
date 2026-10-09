/**
 * Módulo Faturamento PJ: empresa, regime, nota emitida, tomador e parâmetros fiscais.
 *
 * <p>Estrutura (hexagonal): {@code domain} (Java puro), {@code application} (casos de uso e portas),
 * {@code adapter.in.web} e {@code adapter.out.persistence}. Os outros módulos só enxergam a
 * {@code FaturamentoApi} na raiz deste pacote, criada pela primeira feature que precisar dela.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Faturamento PJ")
package dev.bolso.faturamento;
