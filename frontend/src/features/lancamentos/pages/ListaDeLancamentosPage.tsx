import { Link, useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { type SlugDoEspaco, useEspacoAtual } from '@/shared/lib/espacoAtual'
import { useDebounce } from '@/shared/lib/useDebounce'
import { useLancamentosDoMes, useMetas, useTags } from '../api/consultas'
import { diaDe } from '../model/datas'
import { SEM_FILTROS, temFiltro } from '../model/filtros'
import { mesDe, rotuloLongoDoMes } from '../model/mes'
import { BarraDeFiltros } from '../ui/BarraDeFiltros'
import { EstadoDeErro } from '../ui/EstadoDeErro'
import { ListaPorDia } from '../ui/ListaPorDia'
import { ResumoDoMes } from '../ui/ResumoDoMes'
import { SeletorDeMes } from '../ui/SeletorDeMes'

type Props = { espaco: SlugDoEspaco; mes?: string | undefined }

const acao = 'inline-flex h-11 items-center rounded-xl px-5 text-[15px] font-semibold'

export function ListaDeLancamentosPage({ espaco, mes }: Props) {
  const { id: espacoId, tipo } = useEspacoAtual()
  const navegar = useNavigate()
  const agora = new Date()
  const mesAtivo = mes ?? mesDe(agora)

  const [filtros, setFiltros] = useState(SEM_FILTROS)
  const textoAssentado = useDebounce(filtros.texto)
  const consulta = useLancamentosDoMes(espacoId, mesAtivo, { ...filtros, texto: textoAssentado })
  const metas = useMetas(espacoId, tipo === 'HOME')
  const tags = useTags(espacoId)

  return (
    <>
      <header className="flex items-center justify-between gap-3">
        <h1 className="text-[22px] font-bold tracking-tight md:text-[28px]">Lançamentos</h1>
        <SeletorDeMes
          mes={mesAtivo}
          aoMudar={(novo) =>
            void navegar({ to: '/$espaco/lancamentos', params: { espaco }, search: { mes: novo } })
          }
        />
      </header>

      <div className="flex flex-wrap gap-3">
        <Link
          to="/$espaco/lancamentos/novo"
          params={{ espaco }}
          search={{}}
          className={`${acao} bg-primary text-primary-foreground`}
        >
          Novo gasto
        </Link>
        <Link
          to="/$espaco/lancamentos/novo"
          params={{ espaco }}
          search={{ tipo: 'entrada' }}
          className={`${acao} border border-border`}
        >
          Nova entrada
        </Link>
      </div>

      <BarraDeFiltros
        filtros={filtros}
        aoMudar={setFiltros}
        metas={metas.data ?? []}
        tags={tags.data ?? []}
      />

      {consulta.isPending && (
        <p role="status" className="text-[15px] text-muted-foreground">
          Carregando lançamentos…
        </p>
      )}

      {consulta.isError && !consulta.data && (
        <EstadoDeErro
          mensagem="Não foi possível carregar os lançamentos."
          aoTentarDeNovo={() => void consulta.refetch()}
          tentando={consulta.isFetching}
        />
      )}

      {consulta.data && (
        <div className="flex flex-col gap-4" aria-busy={consulta.isPlaceholderData}>
          <ResumoDoMes totais={consulta.data.totais} />
          {consulta.data.itens.length === 0 ? (
            <p className="rounded-2xl border border-border bg-card p-5 text-[15px] text-muted-foreground">
              {temFiltro(filtros)
                ? 'Nenhum lançamento encontrado com esses filtros.'
                : `Nenhum lançamento em ${rotuloLongoDoMes(consulta.data.mes)}.`}
            </p>
          ) : (
            <ListaPorDia
              itens={consulta.data.itens}
              metas={metas.data ?? []}
              espaco={espaco}
              hoje={diaDe(agora)}
            />
          )}
        </div>
      )}
    </>
  )
}
