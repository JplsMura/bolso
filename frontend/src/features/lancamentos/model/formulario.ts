import { z } from 'zod'
import { paraCentavos, paraValorApi } from '@/shared/lib/money'
import type { Direcao, LancamentoApi, NovoLancamentoApi, NovoParcelamentoApi } from './tipos'
import { CODIGOS_OFERECIDOS, ehOferecida } from './pagamentos'
import { MAXIMO_PARCELAS } from './parcelas'

export const MAXIMO_DESCRICAO = 140
export const MAXIMO_TAGS = 10

/**
 * Regras do formulário (as mesmas da API; quem decide de verdade é o servidor). Na Casa toda saída tem meta;
 * na Empresa nenhuma tem. Entrada nunca tem meta nem forma de pagamento. Cartão só com crédito; parcelas só no
 * cartão (1 = à vista). `parcelaAtual` só vale quando a compra já estava em andamento.
 */
export function criarSchema(exigeMeta: boolean) {
  return z
    .object({
      direcao: z.enum(['OUT', 'IN']),
      centavos: z.number().int().positive('Informe um valor maior que zero'),
      descricao: z
        .string()
        .trim()
        .min(1, 'Informe a descrição')
        .max(MAXIMO_DESCRICAO, `Use até ${MAXIMO_DESCRICAO} caracteres`),
      data: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Informe a data'),
      metaId: z.string().nullable(),
      formaPagamento: z.enum(CODIGOS_OFERECIDOS).nullable(),
      cartaoId: z.string().nullable(),
      tagIds: z.array(z.string()).max(MAXIMO_TAGS, `Use no máximo ${MAXIMO_TAGS} tags`),
      parcelas: z
        .number({ error: 'Informe o número de parcelas' })
        .int('Informe o número de parcelas')
        .min(1, 'Informe o número de parcelas')
        .max(MAXIMO_PARCELAS, `Parcele em até ${MAXIMO_PARCELAS} vezes`),
      emAndamento: z.boolean(),
      parcelaAtual: z.number({ error: 'Informe a parcela atual' }).int('Informe a parcela atual'),
    })
    .superRefine((valores, ctx) => {
      if (valores.direcao !== 'OUT') return
      if (exigeMeta && valores.metaId === null) {
        ctx.addIssue({ code: 'custom', path: ['metaId'], message: 'Escolha a meta do gasto' })
      }
      if (valores.formaPagamento === null) {
        ctx.addIssue({ code: 'custom', path: ['formaPagamento'], message: 'Escolha a forma de pagamento' })
      }
      if (valores.formaPagamento === 'CREDIT' && valores.cartaoId === null) {
        ctx.addIssue({ code: 'custom', path: ['cartaoId'], message: 'Escolha o cartão' })
      }
      if (ehParcelado(valores) && valores.centavos > 0 && valores.centavos < valores.parcelas) {
        ctx.addIssue({
          code: 'custom',
          path: ['parcelas'],
          message: 'Cada parcela precisa de pelo menos R$ 0,01',
        })
      }
      if (ehParcelado(valores) && valores.emAndamento) {
        if (valores.parcelaAtual < 1 || valores.parcelaAtual > valores.parcelas) {
          ctx.addIssue({
            code: 'custom',
            path: ['parcelaAtual'],
            message: `A parcela atual vai de 1 a ${valores.parcelas}`,
          })
        }
      }
    })
}

export type ValoresDoFormulario = z.infer<ReturnType<typeof criarSchema>>

/** Gasto no cartão em 2 ou mais vezes: vai para /parcelamentos. */
export const ehParcelado = (v: Pick<ValoresDoFormulario, 'direcao' | 'formaPagamento' | 'parcelas'>) =>
  v.direcao === 'OUT' && v.formaPagamento === 'CREDIT' && v.parcelas > 1

export function valoresIniciais(direcao: Direcao, hoje: string): ValoresDoFormulario {
  return {
    direcao,
    centavos: 0,
    descricao: '',
    data: hoje,
    metaId: null,
    formaPagamento: null,
    cartaoId: null,
    tagIds: [],
    parcelas: 1,
    emAndamento: false,
    parcelaAtual: 1,
  }
}

export function deLancamento(l: LancamentoApi): ValoresDoFormulario {
  const forma = l.formaPagamento && ehOferecida(l.formaPagamento) ? l.formaPagamento : null
  return {
    direcao: l.direcao,
    centavos: paraCentavos(l.valor),
    descricao: l.descricao,
    data: l.data,
    metaId: l.metaId ?? null,
    formaPagamento: forma,
    cartaoId: forma === 'CREDIT' ? (l.cartaoId ?? null) : null,
    tagIds: l.tags.map((t) => t.id),
    parcelas: 1,
    emAndamento: false,
    parcelaAtual: 1,
  }
}

/** O corpo que a API espera. Entrada não leva meta nem forma de pagamento; cartão só no crédito. */
export function paraRequisicao(v: ValoresDoFormulario, comMeta: boolean): NovoLancamentoApi {
  const saida = v.direcao === 'OUT'
  return {
    direcao: v.direcao,
    valor: paraValorApi(v.centavos),
    descricao: v.descricao.trim(),
    data: v.data,
    tagIds: v.tagIds,
    ...(saida && comMeta && v.metaId ? { metaId: v.metaId } : {}),
    ...(saida && v.formaPagamento ? { formaPagamento: v.formaPagamento } : {}),
    ...(saida && v.formaPagamento === 'CREDIT' && v.cartaoId ? { cartaoId: v.cartaoId } : {}),
  }
}

/** O corpo de /parcelamentos: o valor digitado é o total da compra. */
export function paraParcelamento(v: ValoresDoFormulario, comMeta: boolean): NovoParcelamentoApi {
  return {
    valorTotal: paraValorApi(v.centavos),
    parcelas: v.parcelas,
    ...(v.emAndamento ? { parcelaAtual: v.parcelaAtual } : {}),
    descricao: v.descricao.trim(),
    data: v.data,
    cartaoId: v.cartaoId ?? '',
    tagIds: v.tagIds,
    ...(comMeta && v.metaId ? { metaId: v.metaId } : {}),
  }
}
