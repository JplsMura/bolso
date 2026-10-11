import { describe, expect, it } from 'vitest'
import { acoesDaFatura, avisoDaFatura, usoDoLimite } from './fatura'
import { deCartao, paraRequisicao, schemaDoCartao } from './formulario'
import type { CartaoApi, FaturaApi } from './tipos'

const cartao = (usado: string): CartaoApi => ({
  id: 'c1',
  nome: 'Cartão A',
  diaFechamento: 25,
  diaVencimento: 5,
  limite: '1000.00',
  usado,
  disponivel: '0.00',
  versao: 0,
})

const fatura = (status: FaturaApi['status']): FaturaApi => ({
  id: 'f1',
  mes: '2026-03',
  status,
  fechamento: '2026-03-25',
  vencimento: '2026-04-05',
  total: '10.00',
  versao: 0,
  itens: [],
})

describe('uso do limite', () => {
  it('faixa: até 80% normal, acima aviso, acima de 100% perigo; a barra para em 100', () => {
    expect(usoDoLimite(cartao('800.00'))).toEqual({ percentual: 80, faixa: 'normal' })
    expect(usoDoLimite(cartao('810.00'))).toEqual({ percentual: 81, faixa: 'aviso' })
    expect(usoDoLimite(cartao('1200.00'))).toEqual({ percentual: 100, faixa: 'perigo' })
    expect(usoDoLimite(cartao('0.00'))).toEqual({ percentual: 0, faixa: 'normal' })
    expect(usoDoLimite(cartao('1004.00'))).toEqual({ percentual: 100, faixa: 'perigo' }) // 100,4%
    expect(usoDoLimite(cartao('1000.00'))).toEqual({ percentual: 100, faixa: 'aviso' })
    expect(usoDoLimite(cartao('800.01'))).toEqual({ percentual: 80, faixa: 'aviso' })
  })
})

describe('ações e avisos da fatura', () => {
  it('aberta só fecha a partir da data de fechamento', () => {
    expect(acoesDaFatura(fatura('OPEN'), '2026-03-24')).toEqual({})
    expect(acoesDaFatura(fatura('OPEN'), '2026-03-25')).toEqual({ principal: 'fechar' })
  })

  it('fechada paga ou reabre; paga só desfaz', () => {
    expect(acoesDaFatura(fatura('CLOSED'), '2026-03-28')).toEqual({ principal: 'pagar', volta: 'reabrir' })
    expect(acoesDaFatura(fatura('PAID'), '2026-03-28')).toEqual({ volta: 'desfazer-pagamento' })
  })

  it('avisa aberta depois do fechamento e fechada depois do vencimento', () => {
    expect(avisoDaFatura(fatura('OPEN'), '2026-03-20')).toBeNull()
    expect(avisoDaFatura(fatura('OPEN'), '2026-03-25')).toMatch(/passou do fechamento/)
    expect(avisoDaFatura(fatura('CLOSED'), '2026-04-05')).toBeNull()
    expect(avisoDaFatura(fatura('CLOSED'), '2026-04-06')).toMatch(/Venceu em 05\/04/)
    expect(avisoDaFatura(fatura('PAID'), '2026-05-01')).toBeNull()
  })
})

describe('formulário do cartão', () => {
  it('ida e volta com a API, sem float', () => {
    const valores = deCartao(cartao('0.00'))
    expect(valores.limiteCentavos).toBe(100000)
    expect(paraRequisicao({ ...valores, nome: '  Cartão B ' })).toEqual({
      nome: 'Cartão B',
      diaFechamento: 25,
      diaVencimento: 5,
      limite: '1000.00',
    })
  })

  it('dias de 1 a 31, nome até 40 e limite maior que zero', () => {
    const ok = deCartao(cartao('0.00'))
    expect(schemaDoCartao.safeParse(ok).success).toBe(true)
    expect(schemaDoCartao.safeParse({ ...ok, diaFechamento: 32 }).success).toBe(false)
    expect(schemaDoCartao.safeParse({ ...ok, diaVencimento: 0 }).success).toBe(false)
    expect(schemaDoCartao.safeParse({ ...ok, nome: 'x'.repeat(41) }).success).toBe(false)
    expect(schemaDoCartao.safeParse({ ...ok, limiteCentavos: 0 }).success).toBe(false)
  })
})
