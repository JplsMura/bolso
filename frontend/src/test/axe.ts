import axe from 'axe-core'

/** Roda o axe-core no elemento e devolve as violações (contraste não é medido no jsdom). */
export async function violacoesDeAcessibilidade(elemento: Element) {
  const resultado = await axe.run(elemento, { rules: { 'color-contrast': { enabled: false } } })
  return resultado.violations.map((v) => `${v.id}: ${v.help} (${v.nodes.length})`)
}
