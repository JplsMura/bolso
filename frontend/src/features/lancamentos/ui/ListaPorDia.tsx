import { Link } from '@tanstack/react-router'
import { cn } from '@/shared/lib/cn'
import { formatarBRL, paraCentavos } from '@/shared/lib/money'
import type { SlugDoEspaco } from '@/shared/lib/espacoAtual'
import type { LancamentoApi, MetaApi } from '../model/tipos'
import { agruparPorDia } from '../model/agrupar'
import { rotuloDoDia } from '../model/datas'
import { rotuloDaForma } from '../model/pagamentos'

type Props = {
  itens: readonly LancamentoApi[]
  metas: readonly MetaApi[]
  espaco: SlugDoEspaco
  hoje: string
}

const COR_ENTRADA = '#2dd4bf'

export function ListaPorDia({ itens, metas, espaco, hoje }: Props) {
  const metaPorId = new Map(metas.map((m) => [m.id, m]))

  return (
    <div className="flex flex-col gap-1">
      {agruparPorDia(itens).map((grupo) => (
        <section key={grupo.dia} aria-labelledby={`dia-${grupo.dia}`} className="flex flex-col">
          <h2
            id={`dia-${grupo.dia}`}
            className="pb-1.5 pt-2.5 text-xs font-semibold uppercase tracking-[0.8px] text-muted-foreground"
          >
            {rotuloDoDia(grupo.dia, hoje)}
          </h2>
          <ul>
            {grupo.itens.map((item) => (
              <li key={item.id} className="border-t border-popover">
                <Linha
                  item={item}
                  meta={item.metaId ? metaPorId.get(item.metaId) : undefined}
                  espaco={espaco}
                />
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  )
}

function Linha({
  item,
  meta,
  espaco,
}: {
  item: LancamentoApi
  meta: MetaApi | undefined
  espaco: SlugDoEspaco
}) {
  const entrada = item.direcao === 'IN'
  const detalhe = [
    entrada ? 'Entrada' : meta?.nome,
    item.tags.map((t) => t.nome).join(', ') || undefined,
    item.formaPagamento ? rotuloDaForma(item.formaPagamento) : undefined,
  ]
    .filter(Boolean)
    .join(' · ')
  const valor = formatarBRL(paraCentavos(item.valor))

  return (
    <Link
      to="/$espaco/lancamentos/$id"
      params={{ espaco, id: item.id }}
      className="flex min-h-14 items-center gap-3 rounded-lg py-2 hover:bg-card"
    >
      <span
        aria-hidden="true"
        className="size-2.5 shrink-0 rounded-full"
        style={{ backgroundColor: entrada ? COR_ENTRADA : (meta?.cor ?? '#9ba8a5') }}
      />
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="truncate text-[15px] font-medium">{item.descricao}</span>
        <span className="truncate text-xs text-muted-foreground">{detalhe}</span>
      </span>
      <span
        className={cn('whitespace-nowrap text-[15px] font-semibold tabular-nums', entrada && 'text-primary')}
      >
        {entrada ? '+ ' : '− '}
        {valor}
      </span>
    </Link>
  )
}
