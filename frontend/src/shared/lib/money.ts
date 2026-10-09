/**
 * Dinheiro no front: a API manda string com 2 casas ("110.00"), o front trabalha em centavos
 * inteiros e só converte para número decimal na hora de exibir. Nada de parseFloat.
 * Totais e saldos de verdade quem calcula é o back-end.
 */
export type Centavos = number

const VALOR_API = /^(-)?(\d+)(?:\.(\d{1,2}))?$/

/** "110.00" → 11000. Lança erro para qualquer coisa que não seja um decimal com até 2 casas. */
export function paraCentavos(valor: string): Centavos {
  const partes = VALOR_API.exec(valor.trim())
  if (!partes) throw new Error(`Valor monetário inválido: "${valor}"`)

  const [, sinal, inteiro = '0', fracao = ''] = partes
  const centavos = Number(inteiro) * 100 + Number(fracao.padEnd(2, '0'))
  if (!Number.isSafeInteger(centavos)) throw new Error(`Valor monetário fora do limite: "${valor}"`)

  return sinal && centavos !== 0 ? -centavos : centavos
}

/** 11000 → "110.00", formato que a API espera. */
export function paraValorApi(centavos: Centavos): string {
  garantirInteiro(centavos)
  const sinal = centavos < 0 ? '-' : ''
  const abs = Math.abs(centavos)
  const inteiro = Math.trunc(abs / 100)
  const fracao = String(abs % 100).padStart(2, '0')
  return `${sinal}${inteiro}.${fracao}`
}

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

/** 11000 → "R$ 110,00" (com espaço não separável, como o Intl gera). */
export function formatarBRL(centavos: Centavos): string {
  garantirInteiro(centavos)
  return brl.format(centavos / 100)
}

function garantirInteiro(centavos: Centavos) {
  if (!Number.isSafeInteger(centavos)) throw new Error(`Centavos precisam ser inteiros: ${centavos}`)
}
