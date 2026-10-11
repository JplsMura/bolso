import { describe, expect, it } from 'vitest'
import type { LancamentoApi } from './tipos'
import { agruparPorDia } from './agrupar'
import { diaDe, rotuloDoDia } from './datas'
import {
  criarSchema,
  deLancamento,
  ehParcelado,
  paraParcelamento,
  paraRequisicao,
  valoresIniciais,
} from './formulario'
import { dividirEmParcelas, resumoDasParcelas, rotuloDaParcela } from './parcelas'
import { ehMes, mesDe, rotuloCurtoDoMes, rotuloLongoDoMes, somarMeses } from './mes'

describe('meses', () => {
  it('soma e subtrai meses virando o ano', () => {
    expect(somarMeses('2026-12', 1)).toBe('2027-01')
    expect(somarMeses('2026-01', -1)).toBe('2025-12')
    expect(somarMeses('2026-03', 0)).toBe('2026-03')
    expect(somarMeses('2026-03', 25)).toBe('2028-04')
  })

  it('rótulos', () => {
    expect(rotuloCurtoDoMes('2026-10')).toBe('Out 2026')
    expect(rotuloLongoDoMes('2026-03')).toBe('março de 2026')
  })

  it('reconhece só o formato da API', () => {
    expect(ehMes('2026-03')).toBe(true)
    for (const ruim of ['2026-13', '2026-00', '2026-3', 'marco', '', '2026-03-01'])
      expect(ehMes(ruim)).toBe(false)
  })

  it('o mês e o dia vêm do relógio local', () => {
    expect(mesDe(new Date(2026, 0, 31, 23, 59))).toBe('2026-01')
    expect(diaDe(new Date(2026, 2, 5, 0, 1))).toBe('2026-03-05')
  })
})

describe('rótulo do dia', () => {
  it('hoje, ontem e os outros', () => {
    expect(rotuloDoDia('2026-10-08', '2026-10-08')).toBe('Hoje, 8 de outubro')
    expect(rotuloDoDia('2026-10-07', '2026-10-08')).toBe('Ontem, 7 de outubro')
    expect(rotuloDoDia('2026-10-01', '2026-10-08')).toBe('1 de outubro')
  })

  it('ontem atravessa o mês e o ano', () => {
    expect(rotuloDoDia('2026-02-28', '2026-03-01')).toBe('Ontem, 28 de fevereiro')
    expect(rotuloDoDia('2025-12-31', '2026-01-01')).toBe('Ontem, 31 de dezembro')
  })
})

const lanc = (id: string, data: string): LancamentoApi => ({
  id,
  direcao: 'OUT',
  valor: '1.00',
  descricao: id,
  data,
  mesReferencia: data.slice(0, 7),
  tags: [],
  versao: 0,
})

describe('agrupar por dia', () => {
  it('mantém a ordem da API e junta o mesmo dia', () => {
    const grupos = agruparPorDia([lanc('a', '2026-03-11'), lanc('b', '2026-03-11'), lanc('c', '2026-03-05')])
    expect(grupos.map((g) => [g.dia, g.itens.map((i) => i.id)])).toEqual([
      ['2026-03-11', ['a', 'b']],
      ['2026-03-05', ['c']],
    ])
  })

  it('lista vazia, nenhum grupo', () => {
    expect(agruparPorDia([])).toEqual([])
  })
})

describe('regras do formulário', () => {
  const saidaPronta = {
    ...valoresIniciais('OUT', '2026-03-10'),
    centavos: 11000,
    descricao: 'Luz',
    metaId: 'm1',
    formaPagamento: 'PIX' as const,
  }

  it('saída na Casa precisa de valor, descrição, meta e pagamento', () => {
    const resultado = criarSchema(true).safeParse(valoresIniciais('OUT', '2026-03-10'))
    expect(resultado.success).toBe(false)
    const campos = resultado.error?.issues.map((i) => i.path[0])
    expect(campos).toEqual(expect.arrayContaining(['centavos', 'descricao', 'metaId', 'formaPagamento']))
  })

  it('saída pronta passa', () => {
    expect(criarSchema(true).safeParse(saidaPronta).success).toBe(true)
  })

  it('na Empresa a saída não precisa de meta', () => {
    expect(criarSchema(false).safeParse({ ...saidaPronta, metaId: null }).success).toBe(true)
  })

  it('entrada não precisa de meta nem de pagamento', () => {
    const entrada = { ...valoresIniciais('IN', '2026-03-10'), centavos: 500000, descricao: 'Salário' }
    expect(criarSchema(true).safeParse(entrada).success).toBe(true)
  })

  it('descrição só com espaços, valor zero, 11 tags e 141 caracteres não passam', () => {
    const schema = criarSchema(true)
    expect(schema.safeParse({ ...saidaPronta, descricao: '   ' }).success).toBe(false)
    expect(schema.safeParse({ ...saidaPronta, centavos: 0 }).success).toBe(false)
    expect(
      schema.safeParse({ ...saidaPronta, tagIds: Array.from({ length: 11 }, (_, i) => `t${i}`) }).success,
    ).toBe(false)
    expect(schema.safeParse({ ...saidaPronta, descricao: 'a'.repeat(141) }).success).toBe(false)
    expect(schema.safeParse({ ...saidaPronta, descricao: 'a'.repeat(140) }).success).toBe(true)
  })
})

/** Campos de cartão e parcelas de um gasto que não é no cartão. */
const semCartao = { cartaoId: null, parcelas: 1, emAndamento: false, parcelaAtual: 1 }

describe('corpo da requisição', () => {
  it('manda o valor como texto da API, sem float', () => {
    const corpo = paraRequisicao(
      {
        direcao: 'OUT',
        centavos: 11005,
        descricao: '  Luz ',
        data: '2026-03-10',
        metaId: 'm1',
        formaPagamento: 'PIX',
        tagIds: ['t1'],
        ...semCartao,
      },
      true,
    )
    expect(corpo).toEqual({
      direcao: 'OUT',
      valor: '110.05',
      descricao: 'Luz',
      data: '2026-03-10',
      tagIds: ['t1'],
      metaId: 'm1',
      formaPagamento: 'PIX',
    })
  })

  it('entrada não leva meta nem forma de pagamento, mesmo que o estado ainda guarde', () => {
    const corpo = paraRequisicao(
      {
        direcao: 'IN',
        centavos: 100,
        descricao: 'x',
        data: '2026-03-10',
        metaId: 'm1',
        formaPagamento: 'PIX',
        tagIds: [],
        ...semCartao,
      },
      true,
    )
    expect(corpo).not.toHaveProperty('metaId')
    expect(corpo).not.toHaveProperty('formaPagamento')
  })

  it('na Empresa a saída não leva meta', () => {
    const corpo = paraRequisicao(
      {
        direcao: 'OUT',
        centavos: 100,
        descricao: 'x',
        data: '2026-03-10',
        metaId: 'm1',
        formaPagamento: 'BOLETO',
        tagIds: [],
        ...semCartao,
      },
      false,
    )
    expect(corpo).not.toHaveProperty('metaId')
  })

  it('o lançamento da API vira valores do formulário', () => {
    const valores = deLancamento({
      ...lanc('a', '2026-03-10'),
      valor: '89.90',
      metaId: 'm1',
      formaPagamento: 'DEBIT',
      tags: [{ id: 't1', nome: 'Luz' }],
    })
    expect(valores).toMatchObject({ centavos: 8990, metaId: 'm1', formaPagamento: 'DEBIT', tagIds: ['t1'] })
  })
})

describe('cartão e parcelas', () => {
  const noCartao = {
    ...valoresIniciais('OUT', '2026-10-08'),
    centavos: 15000,
    descricao: 'Curso online',
    metaId: 'm1',
    formaPagamento: 'CREDIT' as const,
    cartaoId: 'c1',
  }

  it('crédito exige cartão; outra forma de pagamento não manda cartão', () => {
    const schema = criarSchema(true)
    const semCartaoEscolhido = schema.safeParse({ ...noCartao, cartaoId: null })
    expect(semCartaoEscolhido.success).toBe(false)
    expect(semCartaoEscolhido.error?.issues[0]?.message).toBe('Escolha o cartão')
    expect(schema.safeParse(noCartao).success).toBe(true)

    expect(paraRequisicao(noCartao, true)).toMatchObject({ formaPagamento: 'CREDIT', cartaoId: 'c1' })
    expect(paraRequisicao({ ...noCartao, formaPagamento: 'PIX' }, true)).not.toHaveProperty('cartaoId')
  })

  it('só é parcelado no cartão com 2 ou mais parcelas', () => {
    expect(ehParcelado({ ...noCartao, parcelas: 1 })).toBe(false)
    expect(ehParcelado({ ...noCartao, parcelas: 6 })).toBe(true)
    expect(ehParcelado({ ...noCartao, parcelas: 6, formaPagamento: 'PIX' })).toBe(false)
  })

  it('parcelas de 1 a 48, pelo menos 1 centavo cada e parcela atual dentro do total', () => {
    const schema = criarSchema(true)
    expect(schema.safeParse({ ...noCartao, parcelas: 49 }).success).toBe(false)
    expect(schema.safeParse({ ...noCartao, parcelas: Number.NaN }).success).toBe(false)
    expect(schema.safeParse({ ...noCartao, centavos: 5, parcelas: 6 }).success).toBe(false)
    expect(schema.safeParse({ ...noCartao, parcelas: 6, emAndamento: true, parcelaAtual: 7 }).success).toBe(
      false,
    )
    expect(schema.safeParse({ ...noCartao, parcelas: 6, emAndamento: true, parcelaAtual: 4 }).success).toBe(
      true,
    )
  })

  it('o corpo do parcelamento leva o total e a parcela atual só se em andamento', () => {
    expect(paraParcelamento({ ...noCartao, parcelas: 6 }, true)).toEqual({
      valorTotal: '150.00',
      parcelas: 6,
      descricao: 'Curso online',
      data: '2026-10-08',
      cartaoId: 'c1',
      tagIds: [],
      metaId: 'm1',
    })
    expect(
      paraParcelamento({ ...noCartao, parcelas: 6, emAndamento: true, parcelaAtual: 4 }, false),
    ).toMatchObject({
      parcelaAtual: 4,
    })
    expect(paraParcelamento({ ...noCartao, parcelas: 6 }, false)).not.toHaveProperty('metaId')
  })

  it('divide como o servidor: sobra de centavos na primeira', () => {
    expect(dividirEmParcelas(10000, 3)).toEqual([3334, 3333, 3333])
    expect(dividirEmParcelas(15000, 6)).toEqual([2500, 2500, 2500, 2500, 2500, 2500])
    expect(resumoDasParcelas(15000, 6)).toBe('6x de R$\u00a025,00')
    expect(resumoDasParcelas(10000, 3)).toBe('1x de R$\u00a033,34 + 2x de R$\u00a033,33')
    expect(rotuloDaParcela({ numero: 2, total: 6 })).toBe('2/6')
  })

  it('o gasto no cartão da API volta com o cartão; em outra forma, sem', () => {
    expect(
      deLancamento({ ...lanc('a', '2026-03-10'), formaPagamento: 'CREDIT', cartaoId: 'c1' }),
    ).toMatchObject({
      formaPagamento: 'CREDIT',
      cartaoId: 'c1',
      parcelas: 1,
    })
    expect(deLancamento({ ...lanc('a', '2026-03-10'), formaPagamento: 'PIX' }).cartaoId).toBeNull()
  })
})
