import { Link, useNavigate } from '@tanstack/react-router'
import { diaDe } from '@/shared/lib/datas'
import { mesDe } from '@/shared/lib/mes'
import { type SlugDoEspaco, useEspacoAtual } from '@/shared/lib/espacoAtual'
import { EstadoDeErro } from '@/shared/ui/EstadoDeErro'
import { SeletorDeMes } from '@/shared/ui/SeletorDeMes'
import { useFaturasDoMes, useMetas } from '../api/consultas'
import { BlocoDoCartao } from '../ui/BlocoDoCartao'

type Props = { espaco: SlugDoEspaco; mes?: string | undefined }

/** Cartões e as faturas do mês (design: Cartoes.dc.html). */
export function CartoesPage({ espaco, mes }: Props) {
  const { id: espacoId, tipo } = useEspacoAtual()
  const navegar = useNavigate()
  const agora = new Date()
  const mesAtivo = mes ?? mesDe(agora)
  const consulta = useFaturasDoMes(espacoId, mesAtivo)
  const metas = useMetas(espacoId, tipo === 'HOME')

  return (
    <>
      <header className="flex items-center justify-between gap-3">
        <h1 className="text-[22px] font-bold tracking-tight md:text-[28px]">Cartões</h1>
        <SeletorDeMes
          mes={mesAtivo}
          aoMudar={(novo) =>
            void navegar({ to: '/$espaco/cartoes', params: { espaco }, search: { mes: novo } })
          }
        />
      </header>

      <div className="flex flex-wrap gap-3">
        <Link
          to="/$espaco/cartoes/novo"
          params={{ espaco }}
          className="inline-flex h-11 items-center rounded-xl bg-primary px-5 text-[15px] font-semibold text-primary-foreground"
        >
          Novo cartão
        </Link>
      </div>

      {consulta.isPending && (
        <p role="status" className="text-[15px] text-muted-foreground">
          Carregando cartões…
        </p>
      )}

      {consulta.isError && !consulta.data && (
        <EstadoDeErro
          mensagem="Não foi possível carregar os cartões."
          aoTentarDeNovo={() => void consulta.refetch()}
          tentando={consulta.isFetching}
        />
      )}

      {consulta.data && consulta.data.faturas.length === 0 && (
        <div className="flex flex-col items-start gap-3 rounded-2xl border border-border bg-card p-5">
          <p className="text-[15px]">
            Cadastre seu primeiro cartão para lançar gastos no crédito e acompanhar a fatura.
          </p>
        </div>
      )}

      {consulta.data && consulta.data.faturas.length > 0 && (
        <div className="grid gap-4 lg:grid-cols-2" aria-busy={consulta.isPlaceholderData}>
          {consulta.data.faturas.map((item) => (
            <BlocoDoCartao
              key={item.cartao.id}
              espaco={espaco}
              espacoId={espacoId}
              mes={consulta.data.mes}
              item={item}
              metas={metas.data ?? []}
              hoje={diaDe(agora)}
            />
          ))}
        </div>
      )}

      <p className="text-sm text-muted-foreground">
        Pagar a fatura não cria um novo gasto: os gastos já entram no orçamento pela data da compra.
      </p>
    </>
  )
}
