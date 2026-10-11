import { http, HttpResponse } from 'msw'
import type { components } from '@/shared/api/schema'
import { paraCentavos, paraValorApi } from '@/shared/lib/money'
import { ESPACOS_PADRAO } from './handlers'

type Lancamento = components['schemas']['LancamentoResponse']
type ListaDoMes = components['schemas']['ListaDoMesResponse']

export const CASA_ID = ESPACOS_PADRAO[0].id
export const EMPRESA_ID = ESPACOS_PADRAO[1].id

/** Mesmos ids e cores da migração V5 do backend. */
export const METAS = [
  { id: '019a0000-0000-7000-8000-000000000101', nome: 'Custos Fixos', cor: '#6EA8FE', ordem: 1 },
  { id: '019a0000-0000-7000-8000-000000000102', nome: 'Conforto', cor: '#A3E06B', ordem: 2 },
  { id: '019a0000-0000-7000-8000-000000000103', nome: 'Metas', cor: '#C79BFF', ordem: 3 },
  { id: '019a0000-0000-7000-8000-000000000104', nome: 'Prazeres', cor: '#FF9F5A', ordem: 4 },
  { id: '019a0000-0000-7000-8000-000000000105', nome: 'Liberdade Financeira', cor: '#FF8FB1', ordem: 5 },
  { id: '019a0000-0000-7000-8000-000000000106', nome: 'Conhecimento', cor: '#5CD6E8', ordem: 6 },
] as const

export const [CUSTOS_FIXOS, CONFORTO] = METAS

export const TAG_LUZ = { id: '019a0000-0000-7000-8000-0000000000b1', nome: 'Luz' }
export const TAG_UBER = { id: '019a0000-0000-7000-8000-0000000000b2', nome: 'Uber' }

export const urlDoEspaco = (espacoId: string, caminho: string) => `*/api/v1/espacos/${espacoId}${caminho}`

/** Um lançamento de exemplo (saída de 110,00 na meta Custos Fixos); cada teste sobrescreve o que importa. */
export function lancamentoDe(parcial: Partial<Lancamento> & { id: string }): Lancamento {
  const base: Lancamento = {
    id: parcial.id,
    direcao: 'OUT',
    valor: '110.00',
    descricao: 'Conta de luz',
    data: '2026-03-10',
    mesReferencia: '2026-03',
    metaId: CUSTOS_FIXOS.id,
    formaPagamento: 'PIX',
    tags: [TAG_LUZ],
    versao: 0,
  }
  return { ...base, ...parcial }
}

/** A lista do mês com os totais calculados dos itens (em centavos, como o back-end faz com numeric). */
export function listaDoMes(mes: string, itens: Lancamento[]): ListaDoMes {
  const soma = (d: 'IN' | 'OUT') =>
    itens.filter((i) => i.direcao === d).reduce((total, i) => total + paraCentavos(i.valor), 0)
  const entradas = soma('IN')
  const saidas = soma('OUT')
  return {
    mes,
    totais: {
      entradas: paraValorApi(entradas),
      saidas: paraValorApi(saidas),
      sobra: paraValorApi(entradas - saidas),
    },
    itens,
  }
}

/** Metas e tags do espaço, que todo formulário e toda lista consultam. */
export function handlersDeMetasETags(espacoId: string = CASA_ID, metas: readonly object[] = METAS) {
  return [
    http.get(urlDoEspaco(espacoId, '/metas'), () => HttpResponse.json(metas)),
    http.get(urlDoEspaco(espacoId, '/tags'), () => HttpResponse.json([TAG_LUZ, TAG_UBER])),
  ]
}

/** Responde a lista do mês com `itens` e guarda as URLs pedidas, para o teste conferir os parâmetros. */
export function listaCapturando(itens: Lancamento[], espacoId: string = CASA_ID) {
  const urls: URL[] = []
  const handler = http.get(urlDoEspaco(espacoId, '/lancamentos'), ({ request }) => {
    const url = new URL(request.url)
    urls.push(url)
    return HttpResponse.json(listaDoMes(url.searchParams.get('mes') ?? '2026-03', itens))
  })
  return { handler, urls }
}

export const problema = (status: number, detail?: string) =>
  HttpResponse.json(
    { status, ...(detail ? { detail } : {}) },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
