import { Link, useNavigate } from '@tanstack/react-router'
import { type FormEvent, useState } from 'react'
import { rotuloCurtoDoMes } from '@/shared/lib/mes'
import { formatarBRL, paraCentavos } from '@/shared/lib/money'
import { type SlugDoEspaco, useEspacoAtual } from '@/shared/lib/espacoAtual'
import {
  useCartoes,
  useEditarParcelamento,
  useExcluirParcelamento,
  useMetas,
  useParcelamento,
} from '../api/consultas'
import { ehConflito, ehNaoEncontrado, mensagemDoErro } from '../api/erro'
import { MAXIMO_DESCRICAO } from '../model/formulario'
import type { ParcelamentoApi } from '../model/tipos'
import { CabecalhoDeFormulario } from '../ui/CabecalhoDeFormulario'
import { Chip } from '../ui/Chip'
import { ConfirmarExclusao } from '../ui/ConfirmarExclusao'
import { EstadoDeErro } from '../ui/EstadoDeErro'
import { SeletorDeTags } from '../ui/SeletorDeTags'

type Props = { espaco: SlugDoEspaco; id: string }

const rotulo = 'text-[13px] text-muted-foreground'
const brl = (texto: string) => formatarBRL(paraCentavos(texto))
/** "2026-10-03" → "03/10/2026" */
const dataBr = (data: string) => `${data.slice(8, 10)}/${data.slice(5, 7)}/${data.slice(0, 4)}`

/** Compra parcelada: resumo, parcelas que sobram e a edição (descrição, meta e tags das parcelas em faturas abertas). */
export function ParcelamentoPage({ espaco, id }: Props) {
  const { id: espacoId } = useEspacoAtual()
  const parcelamento = useParcelamento(espacoId, id)

  if (parcelamento.isPending) {
    return (
      <p role="status" className="text-[15px] text-muted-foreground">
        Carregando parcelamento…
      </p>
    )
  }
  if (!parcelamento.data) {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-col gap-5">
        <CabecalhoDeFormulario titulo="Parcelamento" espaco={espaco} />
        {ehNaoEncontrado(parcelamento.error) ? (
          <p role="alert" className="rounded-2xl border border-border bg-card p-5 text-[15px]">
            Parcelamento não encontrado. As parcelas podem ter sido excluídas.
          </p>
        ) : (
          <EstadoDeErro
            mensagem="Não foi possível carregar o parcelamento."
            aoTentarDeNovo={() => void parcelamento.refetch()}
            tentando={parcelamento.isFetching}
          />
        )}
      </div>
    )
  }
  // key: depois de salvar (versão nova), o formulário recomeça com o que o servidor devolveu
  return (
    <Conteudo
      key={`${parcelamento.data.id}-${parcelamento.data.versao}`}
      espaco={espaco}
      espacoId={espacoId}
      atual={parcelamento.data}
      recarregar={() => void parcelamento.refetch()}
      recarregando={parcelamento.isFetching}
    />
  )
}

function Conteudo({
  espaco,
  espacoId,
  atual,
  recarregar,
  recarregando,
}: {
  espaco: SlugDoEspaco
  espacoId: string
  atual: ParcelamentoApi
  recarregar: () => void
  recarregando: boolean
}) {
  const { tipo } = useEspacoAtual()
  const navegar = useNavigate()
  const comMeta = tipo === 'HOME'
  const metas = useMetas(espacoId, comMeta)
  const cartoes = useCartoes(espacoId)
  const editar = useEditarParcelamento(espacoId, atual.id)
  const excluir = useExcluirParcelamento(espacoId, atual.id)

  const [descricao, setDescricao] = useState(atual.descricao)
  const [metaId, setMetaId] = useState<string | null>(atual.metaId ?? null)
  const [tagIds, setTagIds] = useState<string[]>(atual.tags.map((t) => t.id))
  const [erroLocal, setErroLocal] = useState<string | null>(null)
  const [confirmando, setConfirmando] = useState(false)

  const abertas = atual.itens.filter((i) => i.faturaAberta)
  const cartao = cartoes.data?.find((c) => c.id === atual.cartaoId)
  const mesDaPrimeira = atual.itens[0]?.data.slice(0, 7)

  async function salvar(e: FormEvent) {
    e.preventDefault()
    const limpa = descricao.trim()
    if (limpa === '' || limpa.length > MAXIMO_DESCRICAO) {
      setErroLocal(`A descrição deve ter de 1 a ${MAXIMO_DESCRICAO} caracteres`)
      return
    }
    if (comMeta && metaId === null) {
      setErroLocal('Escolha a meta')
      return
    }
    setErroLocal(null)
    try {
      await editar.mutateAsync({
        descricao: limpa,
        ...(comMeta && metaId ? { metaId } : {}),
        tagIds,
        versao: atual.versao,
      })
    } catch {
      // o motivo aparece na tela, a partir de editar.error
    }
  }

  async function confirmarExclusao() {
    try {
      await excluir.mutateAsync()
      await navegar({
        to: '/$espaco/lancamentos',
        params: { espaco },
        search: mesDaPrimeira ? { mes: mesDaPrimeira } : {},
      })
    } catch {
      // o motivo aparece na confirmação
    }
  }

  const conflito = editar.isError && ehConflito(editar.error)

  return (
    <div className="mx-auto flex w-full max-w-lg flex-col gap-5">
      <CabecalhoDeFormulario titulo="Parcelamento" espaco={espaco} mes={mesDaPrimeira} />

      <section
        aria-labelledby="resumo-parcelamento"
        className="flex flex-col gap-1 rounded-2xl border border-border bg-card p-4"
      >
        <h2 id="resumo-parcelamento" className="text-lg font-semibold">
          {atual.descricao}
        </h2>
        <p className="text-sm text-muted-foreground">
          {brl(atual.valorTotal)} em {atual.parcelas}x{cartao ? ` no ${cartao.nome}` : ''} · compra em{' '}
          {dataBr(atual.data)}
        </p>
      </section>

      {conflito && (
        <div
          role="alert"
          className="flex flex-col items-start gap-3 rounded-2xl border border-warning/60 bg-card p-4"
        >
          <p className="text-[15px]">
            Este parcelamento foi alterado em outro lugar. Recarregue e refaça a edição.
          </p>
          <button
            type="button"
            onClick={() => {
              editar.reset()
              recarregar()
            }}
            disabled={recarregando}
            className="h-11 rounded-xl border border-border px-5 text-[15px]"
          >
            Recarregar
          </button>
        </div>
      )}

      {editar.isSuccess && (
        <p role="status" className="text-sm text-primary">
          Parcelas em faturas abertas atualizadas.
        </p>
      )}

      {abertas.length > 0 ? (
        <form onSubmit={(e) => void salvar(e)} noValidate className="flex flex-col gap-5">
          <p className="text-sm text-muted-foreground">
            As mudanças valem para as {abertas.length} parcelas em faturas abertas.
          </p>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="descricao-parcelamento" className={rotulo}>
              Descrição
            </label>
            <input
              id="descricao-parcelamento"
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              className="h-11 w-full rounded-xl border border-border bg-card px-3.5 text-[15px]"
            />
          </div>
          {comMeta && (
            <fieldset className="flex flex-col gap-1.5">
              <legend className={rotulo}>Meta</legend>
              <div className="flex flex-wrap gap-2">
                {metas.data?.map((m) => (
                  <Chip key={m.id} cor={m.cor} marcado={metaId === m.id} aoAlternar={() => setMetaId(m.id)}>
                    {m.nome}
                  </Chip>
                ))}
              </div>
            </fieldset>
          )}
          <SeletorDeTags espacoId={espacoId} selecionadas={tagIds} aoMudar={setTagIds} />
          {(erroLocal || (editar.isError && !conflito)) && (
            <p role="alert" className="text-sm text-destructive">
              {erroLocal ??
                mensagemDoErro(editar.error, 'Não foi possível salvar o parcelamento. Tente de novo.')}
            </p>
          )}
          <div className="flex gap-3">
            <button
              type="submit"
              disabled={editar.isPending}
              className="h-12 flex-1 rounded-xl bg-primary px-5 text-[15px] font-semibold text-primary-foreground disabled:opacity-60"
            >
              {editar.isPending ? 'Salvando…' : 'Salvar alterações'}
            </button>
            <button
              type="button"
              onClick={() => setConfirmando(true)}
              className="h-12 rounded-xl border border-destructive/60 px-5 text-[15px] text-destructive"
            >
              Excluir parcelas restantes
            </button>
          </div>
        </form>
      ) : (
        <p className="rounded-2xl border border-border bg-card p-4 text-[15px]">
          Todas as parcelas que sobram estão em faturas fechadas ou pagas. Para mudar alguma, reabra a fatura
          em{' '}
          <Link to="/$espaco/cartoes" params={{ espaco }} className="text-primary underline">
            Cartões
          </Link>
          .
        </p>
      )}

      {confirmando && (
        <ConfirmarExclusao
          descricao={atual.descricao}
          titulo="Excluir as parcelas restantes?"
          texto={`Saem as ${abertas.length} parcelas de “${atual.descricao}” que estão em faturas abertas. As de faturas fechadas ou pagas ficam.`}
          excluindo={excluir.isPending}
          erro={
            excluir.isError
              ? mensagemDoErro(excluir.error, 'Não foi possível excluir as parcelas.')
              : undefined
          }
          aoCancelar={() => {
            excluir.reset()
            setConfirmando(false)
          }}
          aoConfirmar={() => void confirmarExclusao()}
        />
      )}

      <section aria-labelledby="titulo-parcelas" className="flex flex-col gap-2">
        <h2
          id="titulo-parcelas"
          className="text-xs font-semibold uppercase tracking-[0.8px] text-muted-foreground"
        >
          Parcelas
        </h2>
        <ul className="flex flex-col">
          {atual.itens.map((p) => (
            <li
              key={p.lancamentoId}
              className="flex min-h-12 items-center gap-3 border-t border-popover py-2"
            >
              <span className="w-12 text-sm font-semibold tabular-nums">
                {p.numero}/{atual.parcelas}
              </span>
              <span className="flex flex-1 flex-col">
                <span className="text-[15px] tabular-nums">{brl(p.valor)}</span>
                <span className="text-xs text-muted-foreground">
                  {p.faturaMes ? `Fatura ${rotuloCurtoDoMes(p.faturaMes)}` : 'Sem fatura'} ·{' '}
                  {p.faturaAberta ? 'aberta' : 'fechada ou paga'}
                </span>
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
