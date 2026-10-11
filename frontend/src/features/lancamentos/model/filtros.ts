import type { Direcao, FormaPagamento } from './tipos'

/** Filtros da lista do mês. `texto` é a busca digitada (pode estar em branco). */
export type Filtros = {
  texto: string
  metaId: string | null
  tagId: string | null
  pagamento: FormaPagamento | null
  direcao: Direcao | null
}

export const SEM_FILTROS: Filtros = { texto: '', metaId: null, tagId: null, pagamento: null, direcao: null }

export function temFiltro(filtros: Filtros): boolean {
  return (
    filtros.texto.trim() !== '' ||
    filtros.metaId !== null ||
    filtros.tagId !== null ||
    filtros.pagamento !== null ||
    filtros.direcao !== null
  )
}
