/**
 * Módulo Identidade: usuário, espaço (Casa ou Empresa), membro e papel; login por código e Google.
 *
 * <p>Estrutura (hexagonal): {@code domain} (Java puro), {@code application} (casos de uso e portas),
 * {@code adapter.in.web} e {@code adapter.out.persistence}. Os outros módulos só enxergam a
 * {@code IdentidadeApi} na raiz deste pacote, criada pela primeira feature que precisar dela.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Identidade")
package dev.bolso.identidade;
