import { http, HttpResponse } from 'msw'
import type { components } from '@/shared/api/schema'
import { CASA_ID, CONFORTO, CUSTOS_FIXOS, urlDoEspaco } from './lancamentos'

type Cartao = components['schemas']['CartaoResponse']
type Fatura = components['schemas']['FaturaResponse']
type FaturaDoCartao = components['schemas']['FaturaDoCartaoResponse']

/** Dados fictícios, os mesmos do design. */
export const NUBANK: Cartao = {
  id: '019a0000-0000-7000-8000-0000000000c1',
  nome: 'Nubank',
  diaFechamento: 25,
  diaVencimento: 5,
  limite: '4500.00',
  usado: '1260.00',
  disponivel: '3240.00',
  versao: 0,
}

export const BTG: Cartao = {
  id: '019a0000-0000-7000-8000-0000000000c2',
  nome: 'BTG',
  diaFechamento: 5,
  diaVencimento: 15,
  limite: '8000.00',
  usado: '2900.00',
  disponivel: '5100.00',
  versao: 0,
}

export function faturaDe(parcial: Partial<Fatura> & { id: string }): Fatura {
  return {
    mes: '2026-03',
    status: 'OPEN',
    fechamento: '2026-03-25',
    vencimento: '2026-04-05',
    total: '88.80',
    versao: 0,
    itens: [
      {
        lancamentoId: '019a0000-0000-7000-8000-00000000d001',
        descricao: 'Uber',
        data: '2026-03-10',
        metaId: CONFORTO.id,
        valor: '23.90',
      },
      {
        lancamentoId: '019a0000-0000-7000-8000-00000000d002',
        descricao: 'Curso online',
        data: '2026-03-03',
        metaId: CUSTOS_FIXOS.id,
        valor: '25.00',
        parcela: { numero: 2, total: 6 },
      },
      {
        lancamentoId: '019a0000-0000-7000-8000-00000000d003',
        descricao: 'Streaming',
        data: '2026-03-01',
        metaId: CONFORTO.id,
        valor: '39.90',
      },
    ],
    ...parcial,
  }
}

export const FATURA_NUBANK = faturaDe({ id: '019a0000-0000-7000-8000-0000000000f1' })

/** GET /faturas: responde `faturas` e guarda as URLs pedidas. */
export function faturasCapturando(faturas: FaturaDoCartao[], espacoId: string = CASA_ID) {
  const urls: URL[] = []
  const handler = http.get(urlDoEspaco(espacoId, '/faturas'), ({ request }) => {
    const url = new URL(request.url)
    urls.push(url)
    return HttpResponse.json({ mes: url.searchParams.get('mes') ?? '2026-03', faturas })
  })
  return { handler, urls }
}

export const cartoesNaCasa = (cartoes: Cartao[]) =>
  http.get(urlDoEspaco(CASA_ID, '/cartoes'), () => HttpResponse.json(cartoes))
