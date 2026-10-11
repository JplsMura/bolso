import { paraCentavos } from '@/shared/lib/money'
import type { AcaoDaFatura, CartaoApi, FaturaApi, StatusDaFatura } from './tipos'

export const ETAPAS: readonly { status: StatusDaFatura; rotulo: string }[] = [
  { status: 'OPEN', rotulo: 'Aberta' },
  { status: 'CLOSED', rotulo: 'Fechada' },
  { status: 'PAID', rotulo: 'Paga' },
]

/** "2026-10-25" → "25/10" */
export const diaEMes = (data: string) => `${data.slice(8, 10)}/${data.slice(5, 7)}`

/** A ação principal de cada status (a do botão grande) e a de volta (no botão secundário, com confirmação). */
export function acoesDaFatura(
  fatura: FaturaApi,
  hoje: string,
): { principal?: AcaoDaFatura; volta?: AcaoDaFatura } {
  switch (fatura.status) {
    case 'OPEN':
      // antes do fechamento o servidor recusa; o botão nem aparece
      return hoje >= fatura.fechamento ? { principal: 'fechar' } : {}
    case 'CLOSED':
      return { principal: 'pagar', volta: 'reabrir' }
    case 'PAID':
      return { volta: 'desfazer-pagamento' }
  }
}

export const ROTULO_DA_ACAO: Record<AcaoDaFatura, string> = {
  fechar: 'Fechar fatura',
  pagar: 'Marcar como paga',
  reabrir: 'Reabrir fatura',
  'desfazer-pagamento': 'Desfazer pagamento',
}

/** Aviso de prazo: aberta depois do fechamento, ou fechada depois do vencimento. */
export function avisoDaFatura(fatura: FaturaApi, hoje: string): string | null {
  if (fatura.status === 'OPEN' && hoje >= fatura.fechamento) {
    return `Já passou do fechamento (${diaEMes(fatura.fechamento)}): feche a fatura quando conferir com o banco.`
  }
  if (fatura.status === 'CLOSED' && hoje > fatura.vencimento) {
    return `Venceu em ${diaEMes(fatura.vencimento)} e ainda não foi marcada como paga.`
  }
  return null
}

export type FaixaDoUso = 'normal' | 'aviso' | 'perigo'

/** Quanto do limite está usado, de 0 a 100 (a barra) e a faixa da cor: aviso acima de 80%, perigo acima de 100%. */
export function usoDoLimite(cartao: CartaoApi): { percentual: number; faixa: FaixaDoUso } {
  const limite = paraCentavos(cartao.limite)
  const usado = paraCentavos(cartao.usado)
  const percentual = limite > 0 ? Math.round((usado / limite) * 100) : 0
  // a faixa compara os centavos (o percentual arredondado esconderia 100,4% como 100%)
  const faixa: FaixaDoUso = usado > limite ? 'perigo' : usado * 100 > limite * 80 ? 'aviso' : 'normal'
  return { percentual: Math.max(0, Math.min(100, percentual)), faixa }
}
