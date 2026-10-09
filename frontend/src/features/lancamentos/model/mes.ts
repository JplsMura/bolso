/** Meses no formato da API ("2026-03"). Só cálculo de calendário, sem fuso: o mês é um texto. */

const ABREVIADOS = [
  'Jan',
  'Fev',
  'Mar',
  'Abr',
  'Mai',
  'Jun',
  'Jul',
  'Ago',
  'Set',
  'Out',
  'Nov',
  'Dez',
] as const
const POR_EXTENSO = [
  'janeiro',
  'fevereiro',
  'março',
  'abril',
  'maio',
  'junho',
  'julho',
  'agosto',
  'setembro',
  'outubro',
  'novembro',
  'dezembro',
] as const

const FORMATO = /^(\d{4})-(0[1-9]|1[0-2])$/

export function ehMes(texto: string): boolean {
  return FORMATO.test(texto)
}

function partes(mes: string): [number, number] {
  const m = FORMATO.exec(mes)
  if (!m) throw new Error(`Mês inválido: "${mes}"`)
  return [Number(m[1]), Number(m[2])]
}

/** O mês do relógio local de quem está usando. */
export function mesDe(data: Date): string {
  return `${String(data.getFullYear()).padStart(4, '0')}-${String(data.getMonth() + 1).padStart(2, '0')}`
}

export function somarMeses(mes: string, quantos: number): string {
  const [ano, numero] = partes(mes)
  const indice = ano * 12 + (numero - 1) + quantos
  const novoAno = Math.floor(indice / 12)
  return `${String(novoAno).padStart(4, '0')}-${String((indice % 12) + 1).padStart(2, '0')}`
}

/** "2026-10" → "Out 2026" */
export function rotuloCurtoDoMes(mes: string): string {
  const [ano, numero] = partes(mes)
  return `${ABREVIADOS[numero - 1]} ${ano}`
}

/** "2026-03" → "março de 2026" */
export function rotuloLongoDoMes(mes: string): string {
  const [ano, numero] = partes(mes)
  return `${POR_EXTENSO[numero - 1]} de ${ano}`
}
