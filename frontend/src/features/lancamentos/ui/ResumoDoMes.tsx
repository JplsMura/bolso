import { formatarBRL, paraCentavos } from '@/shared/lib/money'
import { cn } from '@/shared/lib/cn'
import type { ListaDoMesApi } from '../model/tipos'

const brl = (texto: string) => formatarBRL(paraCentavos(texto))

/** Entradas, saídas e sobra do mês inteiro (os filtros não mudam estes números). */
export function ResumoDoMes({ totais }: { totais: ListaDoMesApi['totais'] }) {
  const negativa = totais.sobra.startsWith('-')
  return (
    <dl className="grid grid-cols-3 gap-2 rounded-2xl border border-border bg-card p-3 text-center">
      <Total rotulo="Entradas" valor={brl(totais.entradas)} />
      <Total rotulo="Saídas" valor={brl(totais.saidas)} />
      <Total
        rotulo="Sobra"
        valor={brl(totais.sobra)}
        destaque={negativa ? 'text-destructive' : 'text-primary'}
      />
    </dl>
  )
}

function Total({ rotulo, valor, destaque }: { rotulo: string; valor: string; destaque?: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs text-muted-foreground">{rotulo}</dt>
      <dd className={cn('text-[15px] font-semibold tabular-nums', destaque)}>{valor}</dd>
    </div>
  )
}
