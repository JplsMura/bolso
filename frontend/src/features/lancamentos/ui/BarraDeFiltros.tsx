import { cn } from '@/shared/lib/cn'
import type { FormaPagamento, MetaApi, TagApi, Direcao } from '../model/tipos'
import { type Filtros, SEM_FILTROS, temFiltro } from '../model/filtros'
import { FORMAS_OFERECIDAS } from '../model/pagamentos'

type Props = {
  filtros: Filtros
  aoMudar: (filtros: Filtros) => void
  metas: readonly MetaApi[]
  tags: readonly TagApi[]
}

const chip = 'h-9 rounded-full bg-popover px-3.5 text-[13px] text-foreground'

export function BarraDeFiltros({ filtros, aoMudar, metas, tags }: Props) {
  const nenhum = !temFiltro(filtros)
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-1.5">
        <label htmlFor="busca" className="sr-only">
          Buscar lançamentos
        </label>
        <input
          id="busca"
          type="search"
          value={filtros.texto}
          onChange={(e) => aoMudar({ ...filtros, texto: e.target.value })}
          placeholder="Buscar, por exemplo: uber"
          className="h-11 w-full rounded-xl border border-border bg-card px-3.5 text-[15px] placeholder:text-muted-foreground"
        />
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          aria-pressed={nenhum}
          onClick={() => aoMudar(SEM_FILTROS)}
          className={cn(chip, 'font-semibold', nenhum && 'bg-primary text-primary-foreground')}
        >
          Todos
        </button>

        <select
          aria-label="Tipo"
          className={chip}
          value={filtros.direcao ?? ''}
          onChange={(e) =>
            aoMudar({ ...filtros, direcao: e.target.value === '' ? null : (e.target.value as Direcao) })
          }
        >
          <option value="">Tipo</option>
          <option value="OUT">Saídas</option>
          <option value="IN">Entradas</option>
        </select>

        {metas.length > 0 && (
          <select
            aria-label="Meta"
            className={chip}
            value={filtros.metaId ?? ''}
            onChange={(e) => aoMudar({ ...filtros, metaId: e.target.value === '' ? null : e.target.value })}
          >
            <option value="">Meta</option>
            {metas.map((m) => (
              <option key={m.id} value={m.id}>
                {m.nome}
              </option>
            ))}
          </select>
        )}

        <select
          aria-label="Tag"
          className={chip}
          value={filtros.tagId ?? ''}
          onChange={(e) => aoMudar({ ...filtros, tagId: e.target.value === '' ? null : e.target.value })}
        >
          <option value="">Tag</option>
          {tags.map((t) => (
            <option key={t.id} value={t.id}>
              {t.nome}
            </option>
          ))}
        </select>

        <select
          aria-label="Pagamento"
          className={chip}
          value={filtros.pagamento ?? ''}
          onChange={(e) =>
            aoMudar({
              ...filtros,
              pagamento: e.target.value === '' ? null : (e.target.value as FormaPagamento),
            })
          }
        >
          <option value="">Pagamento</option>
          {FORMAS_OFERECIDAS.map((f) => (
            <option key={f.valor} value={f.valor}>
              {f.rotulo}
            </option>
          ))}
        </select>
      </div>
    </div>
  )
}
