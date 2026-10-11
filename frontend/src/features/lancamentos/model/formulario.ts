import { z } from 'zod'
import { paraCentavos, paraValorApi } from '@/shared/lib/money'
import type { Direcao, LancamentoApi, NovoLancamentoApi } from './tipos'
import { CODIGOS_OFERECIDOS, ehOferecida } from './pagamentos'

export const MAXIMO_DESCRICAO = 140
export const MAXIMO_TAGS = 10

/**
 * Regras do formulário (as mesmas da API; quem decide de verdade é o servidor). Na Casa toda saída tem meta;
 * na Empresa nenhuma tem. Entrada nunca tem meta nem forma de pagamento.
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
      tagIds: z.array(z.string()).max(MAXIMO_TAGS, `Use no máximo ${MAXIMO_TAGS} tags`),
    })
    .superRefine((valores, ctx) => {
      if (valores.direcao !== 'OUT') return
      if (exigeMeta && valores.metaId === null) {
        ctx.addIssue({ code: 'custom', path: ['metaId'], message: 'Escolha a meta do gasto' })
      }
      if (valores.formaPagamento === null) {
        ctx.addIssue({ code: 'custom', path: ['formaPagamento'], message: 'Escolha a forma de pagamento' })
      }
    })
}

export type ValoresDoFormulario = z.infer<ReturnType<typeof criarSchema>>

export function valoresIniciais(direcao: Direcao, hoje: string): ValoresDoFormulario {
  return { direcao, centavos: 0, descricao: '', data: hoje, metaId: null, formaPagamento: null, tagIds: [] }
}

export function deLancamento(l: LancamentoApi): ValoresDoFormulario {
  return {
    direcao: l.direcao,
    centavos: paraCentavos(l.valor),
    descricao: l.descricao,
    data: l.data,
    metaId: l.metaId ?? null,
    // CREDIT só existe a partir da feature de cartões; a tela não o oferece
    formaPagamento: l.formaPagamento && ehOferecida(l.formaPagamento) ? l.formaPagamento : null,
    tagIds: l.tags.map((t) => t.id),
  }
}

/** O corpo que a API espera. Entrada não leva meta nem forma de pagamento. */
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
  }
}
