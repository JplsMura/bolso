/** Datas da lista. A API manda só o dia ("2026-10-08"), então nada aqui passa por fuso horário. */

const MESES = [
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

const FORMATO = /^(\d{4})-(\d{2})-(\d{2})$/

function partes(dia: string): [number, number, number] {
  const m = FORMATO.exec(dia)
  if (!m) throw new Error(`Data inválida: "${dia}"`)
  return [Number(m[1]), Number(m[2]), Number(m[3])]
}

/** O dia do relógio local, no formato da API. */
export function diaDe(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0')
  const dia = String(data.getDate()).padStart(2, '0')
  return `${String(data.getFullYear()).padStart(4, '0')}-${mes}-${dia}`
}

function diaAnterior(dia: string): string {
  const [ano, mes, numero] = partes(dia)
  const anterior = new Date(Date.UTC(ano, mes - 1, numero - 1))
  return `${String(anterior.getUTCFullYear()).padStart(4, '0')}-${String(anterior.getUTCMonth() + 1).padStart(2, '0')}-${String(anterior.getUTCDate()).padStart(2, '0')}`
}

/** "Hoje, 8 de outubro", "Ontem, 7 de outubro" ou "1 de outubro". */
export function rotuloDoDia(dia: string, hoje: string): string {
  const [, mes, numero] = partes(dia)
  const texto = `${numero} de ${MESES[mes - 1]}`
  if (dia === hoje) return `Hoje, ${texto}`
  if (dia === diaAnterior(hoje)) return `Ontem, ${texto}`
  return texto
}
