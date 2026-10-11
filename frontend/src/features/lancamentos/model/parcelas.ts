import { type Centavos, formatarBRL } from '@/shared/lib/money'

export const MINIMO_PARCELAS = 2
export const MAXIMO_PARCELAS = 48

/**
 * Como o servidor divide o total: os centavos que sobram vão na primeira parcela.
 * 10000 em 3 → [3334, 3333, 3333].
 */
export function dividirEmParcelas(total: Centavos, parcelas: number): Centavos[] {
  const base = Math.floor(total / parcelas)
  const resto = total - base * parcelas
  return Array.from({ length: parcelas }, (_, i) => (i === 0 ? base + resto : base))
}

/** "6x de R$ 25,00", ou "1x de R$ 33,34 + 2x de R$ 33,33" quando a divisão não é exata. */
export function resumoDasParcelas(total: Centavos, parcelas: number): string {
  const [primeira = 0, outra = 0] = dividirEmParcelas(total, parcelas)
  if (primeira === outra) return `${parcelas}x de ${formatarBRL(outra)}`
  return `1x de ${formatarBRL(primeira)} + ${parcelas - 1}x de ${formatarBRL(outra)}`
}

/** "2/6" */
export const rotuloDaParcela = (parcela: { numero: number; total: number }) =>
  `${parcela.numero}/${parcela.total}`
