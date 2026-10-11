import type { FormaPagamento } from './tipos'

/** Códigos que a tela oferece, na ordem do design (Cartão primeiro). */
export const CODIGOS_OFERECIDOS = ['CREDIT', 'PIX', 'DEBIT', 'CASH', 'BOLETO', 'TRANSFER', 'OTHER'] as const
export type FormaOferecida = (typeof CODIGOS_OFERECIDOS)[number]

export const ehOferecida = (forma: FormaPagamento): forma is FormaOferecida =>
  (CODIGOS_OFERECIDOS as readonly string[]).includes(forma)

export const FORMAS_OFERECIDAS: readonly { valor: FormaOferecida; rotulo: string }[] = [
  { valor: 'CREDIT', rotulo: 'Cartão' },
  { valor: 'PIX', rotulo: 'PIX' },
  { valor: 'DEBIT', rotulo: 'Débito' },
  { valor: 'CASH', rotulo: 'Dinheiro' },
  { valor: 'BOLETO', rotulo: 'Boleto' },
  { valor: 'TRANSFER', rotulo: 'Transferência' },
  { valor: 'OTHER', rotulo: 'Outro' },
]

const ROTULOS: Record<FormaPagamento, string> = {
  PIX: 'PIX',
  DEBIT: 'Débito',
  CREDIT: 'Cartão',
  CASH: 'Dinheiro',
  BOLETO: 'Boleto',
  TRANSFER: 'Transferência',
  OTHER: 'Outro',
}

export const rotuloDaForma = (forma: FormaPagamento) => ROTULOS[forma]
