import { z } from 'zod'
import { paraCentavos, paraValorApi } from '@/shared/lib/money'
import type { CartaoApi, NovoCartaoApi } from './tipos'

export const MAXIMO_NOME = 40

const dia = (campo: string) =>
  z
    .number({ error: `Informe o dia de ${campo}` })
    .int(`Informe o dia de ${campo}`)
    .min(1, 'Use um dia de 1 a 31')
    .max(31, 'Use um dia de 1 a 31')

/** As mesmas regras da API (quem decide de verdade é o servidor). */
export const schemaDoCartao = z.object({
  nome: z
    .string()
    .trim()
    .min(1, 'Informe o nome do cartão')
    .max(MAXIMO_NOME, `Use até ${MAXIMO_NOME} caracteres`),
  diaFechamento: dia('fechamento'),
  diaVencimento: dia('vencimento'),
  limiteCentavos: z.number().int().positive('Informe um limite maior que zero'),
})

export type ValoresDoCartao = z.infer<typeof schemaDoCartao>

export const CARTAO_VAZIO: ValoresDoCartao = {
  nome: '',
  diaFechamento: Number.NaN,
  diaVencimento: Number.NaN,
  limiteCentavos: 0,
}

export function deCartao(c: CartaoApi): ValoresDoCartao {
  return {
    nome: c.nome,
    diaFechamento: c.diaFechamento,
    diaVencimento: c.diaVencimento,
    limiteCentavos: paraCentavos(c.limite),
  }
}

export function paraRequisicao(v: ValoresDoCartao): NovoCartaoApi {
  return {
    nome: v.nome.trim(),
    diaFechamento: v.diaFechamento,
    diaVencimento: v.diaVencimento,
    limite: paraValorApi(v.limiteCentavos),
  }
}
