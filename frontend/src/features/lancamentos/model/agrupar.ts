import type { LancamentoApi } from './tipos'

export type GrupoDoDia = { dia: string; itens: LancamentoApi[] }

/** Agrupa por dia mantendo a ordem que a API mandou (do mais recente para o mais antigo). */
export function agruparPorDia(itens: readonly LancamentoApi[]): GrupoDoDia[] {
  const grupos: GrupoDoDia[] = []
  for (const item of itens) {
    const ultimo = grupos[grupos.length - 1]
    if (ultimo && ultimo.dia === item.data) ultimo.itens.push(item)
    else grupos.push({ dia: item.data, itens: [item] })
  }
  return grupos
}
