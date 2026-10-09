import { createContext, useContext } from 'react'

export type TipoEspaco = 'HOME' | 'COMPANY'

/** O espaço da URL (/casa, /empresa), já resolvido pela API. As features usam o `id` nas chamadas. */
export type EspacoAtual = { id: string; tipo: TipoEspaco; nome: string }

/** Preenchido pelo layout em `app`; vive em `shared` porque uma feature não importa outra. */
export const EspacoAtualContext = createContext<EspacoAtual | null>(null)

export function useEspacoAtual(): EspacoAtual {
  const espaco = useContext(EspacoAtualContext)
  if (!espaco) {
    throw new Error('useEspacoAtual só funciona dentro do layout de um espaço (/casa ou /empresa).')
  }
  return espaco
}
