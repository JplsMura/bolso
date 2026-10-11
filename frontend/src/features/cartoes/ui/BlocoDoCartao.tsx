import { Link } from '@tanstack/react-router'
import { useState } from 'react'
import { cn } from '@/shared/lib/cn'
import { rotuloLongoDoMes } from '@/shared/lib/mes'
import { formatarBRL, paraCentavos } from '@/shared/lib/money'
import type { SlugDoEspaco } from '@/shared/lib/espacoAtual'
import { ehConflito, mensagemDoErro, useMudarFatura } from '../api/consultas'
import { acoesDaFatura, avisoDaFatura, diaEMes, ETAPAS, ROTULO_DA_ACAO, usoDoLimite } from '../model/fatura'
import type { AcaoDaFatura, FaturaApi, FaturaDoCartaoApi, MetaApi } from '../model/tipos'
import { Confirmacao } from './Confirmacao'

type Props = {
  espaco: SlugDoEspaco
  espacoId: string
  mes: string
  item: FaturaDoCartaoApi
  metas: readonly MetaApi[]
  hoje: string
}

const brl = (texto: string) => formatarBRL(paraCentavos(texto))

const COR_DA_FAIXA = { normal: 'bg-primary', aviso: 'bg-warning', perigo: 'bg-destructive' } as const

/** Um cartão: limite usado e disponível, e a fatura do mês com as etapas, os gastos e a próxima ação. */
export function BlocoDoCartao({ espaco, espacoId, mes, item, metas, hoje }: Props) {
  const { cartao, fatura } = item
  const uso = usoDoLimite(cartao)
  const negativo = cartao.disponivel.startsWith('-')
  const idTitulo = `cartao-${cartao.id}`

  return (
    <section
      aria-labelledby={idTitulo}
      className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-4 md:p-5"
    >
      <header className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-0.5">
          <h2 id={idTitulo} className="text-lg font-semibold">
            {cartao.nome}
          </h2>
          <p className="text-sm text-muted-foreground">
            Fecha dia {cartao.diaFechamento} · vence dia {cartao.diaVencimento}
          </p>
        </div>
        <Link
          to="/$espaco/cartoes/$id"
          params={{ espaco, id: cartao.id }}
          className="inline-flex h-11 items-center rounded-xl px-3 text-sm text-muted-foreground hover:bg-popover hover:text-foreground"
        >
          Editar<span className="sr-only"> {cartao.nome}</span>
        </Link>
      </header>

      <div className="flex flex-col gap-2">
        <div className="flex items-baseline justify-between gap-3 text-sm">
          <span className="text-muted-foreground">Limite usado</span>
          <span className="tabular-nums">
            {brl(cartao.usado)} de {brl(cartao.limite)}
          </span>
        </div>
        <div
          role="meter"
          aria-label={`Limite usado do ${cartao.nome}`}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={uso.percentual}
          aria-valuetext={`${uso.percentual}% do limite`}
          className="h-2 overflow-hidden rounded-full bg-popover"
        >
          <div className={cn('h-2', COR_DA_FAIXA[uso.faixa])} style={{ width: `${uso.percentual}%` }} />
        </div>
        <div className="flex items-baseline justify-between gap-3 text-sm">
          <span className="text-muted-foreground">Disponível</span>
          <span className={cn('font-semibold tabular-nums', negativo && 'text-destructive')}>
            {brl(cartao.disponivel)}
            {negativo && ' (passou do limite)'}
          </span>
        </div>
      </div>

      {fatura ? (
        <Fatura espacoId={espacoId} nomeDoCartao={cartao.nome} fatura={fatura} metas={metas} hoje={hoje} />
      ) : (
        <p className="border-t border-popover pt-3 text-sm text-muted-foreground">
          Sem gastos na fatura de {rotuloLongoDoMes(mes)}.
        </p>
      )}
    </section>
  )
}

function Fatura({
  espacoId,
  nomeDoCartao,
  fatura,
  metas,
  hoje,
}: {
  espacoId: string
  nomeDoCartao: string
  fatura: FaturaApi
  metas: readonly MetaApi[]
  hoje: string
}) {
  const mudar = useMudarFatura(espacoId)
  const [pagando, setPagando] = useState(false)
  const [pagaEm, setPagaEm] = useState(hoje)
  const [confirmando, setConfirmando] = useState<AcaoDaFatura | null>(null)
  const { principal, volta } = acoesDaFatura(fatura, hoje)
  const aviso = avisoDaFatura(fatura, hoje)
  const metaPorId = new Map(metas.map((m) => [m.id, m]))
  const mesExtenso = rotuloLongoDoMes(fatura.mes)

  async function aplicar(acao: AcaoDaFatura) {
    try {
      await mudar.mutateAsync({
        faturaId: fatura.id,
        acao,
        versao: fatura.versao,
        ...(acao === 'pagar' ? { pagaEm } : {}),
      })
      setPagando(false)
      setConfirmando(null)
    } catch {
      // o motivo aparece abaixo, a partir de mudar.error
    }
  }

  const erro = mudar.isError
    ? ehConflito(mudar.error)
      ? 'A fatura mudou em outro lugar. A tela já foi atualizada; confira e tente de novo.'
      : mensagemDoErro(mudar.error, 'Não foi possível mudar a fatura. Tente de novo.')
    : undefined

  return (
    <div className="flex flex-col gap-3 border-t border-popover pt-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-[15px] font-semibold">Fatura de {mesExtenso}</h3>
        <ol aria-label="Etapas da fatura" className="flex gap-1.5">
          {ETAPAS.map((e) => {
            const atual = e.status === fatura.status
            return (
              <li
                key={e.status}
                aria-current={atual ? 'step' : undefined}
                className={cn(
                  'inline-flex h-7 items-center rounded-full border-[1.5px] px-3 text-[13px]',
                  atual
                    ? 'border-primary bg-popover font-semibold text-foreground'
                    : 'border-border text-muted-foreground',
                )}
              >
                {e.rotulo}
              </li>
            )
          })}
        </ol>
      </div>

      <p className="text-sm text-muted-foreground">
        Fecha {diaEMes(fatura.fechamento)} · vence {diaEMes(fatura.vencimento)}
        {fatura.pagaEm && ` · paga em ${diaEMes(fatura.pagaEm)}`}
      </p>

      {aviso && <p className="text-sm text-warning">{aviso}</p>}

      <dl className="flex items-baseline justify-between">
        <dt className="text-sm text-muted-foreground">
          {fatura.status === 'OPEN' ? 'Total até agora' : 'Total da fatura'}
        </dt>
        <dd className="text-xl font-bold tabular-nums">{brl(fatura.total)}</dd>
      </dl>

      {fatura.itens.length > 0 && (
        <ul aria-label={`Gastos da fatura de ${mesExtenso}`} className="flex flex-col">
          {fatura.itens.map((i) => {
            const meta = i.metaId ? metaPorId.get(i.metaId) : undefined
            return (
              <li
                key={i.lancamentoId}
                className="flex min-h-11 items-center gap-3 border-t border-popover py-1.5"
              >
                <span
                  aria-hidden="true"
                  className="size-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: meta?.cor ?? '#9ba8a5' }}
                />
                <span className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate text-[15px]">
                    {i.descricao}
                    {i.parcela && (
                      <span className="text-muted-foreground">
                        {' '}
                        {i.parcela.numero}/{i.parcela.total}
                      </span>
                    )}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {diaEMes(i.data)}
                    {meta && ` · ${meta.nome}`}
                  </span>
                </span>
                <span className="whitespace-nowrap text-[15px] tabular-nums">{brl(i.valor)}</span>
              </li>
            )
          })}
        </ul>
      )}

      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}

      {pagando && (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            void aplicar('pagar')
          }}
          className="flex flex-col gap-2 rounded-xl border border-border p-3"
        >
          <label htmlFor={`pagamento-${fatura.id}`} className="text-[13px] text-muted-foreground">
            Data do pagamento
          </label>
          <input
            id={`pagamento-${fatura.id}`}
            type="date"
            value={pagaEm}
            min={fatura.fechamento}
            max={hoje}
            onChange={(e) => setPagaEm(e.target.value)}
            className="h-11 w-full rounded-xl border border-border bg-card px-3.5 text-[15px]"
          />
          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => setPagando(false)}
              className="h-11 flex-1 rounded-xl border border-border px-4 text-[15px]"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={mudar.isPending}
              className="h-11 flex-1 rounded-xl bg-primary px-4 text-[15px] font-semibold text-primary-foreground disabled:opacity-60"
            >
              {mudar.isPending ? 'Salvando…' : 'Confirmar pagamento'}
            </button>
          </div>
        </form>
      )}

      {confirmando && (
        <Confirmacao
          titulo={confirmando === 'reabrir' ? 'Reabrir esta fatura?' : 'Desfazer o pagamento?'}
          texto={
            confirmando === 'reabrir'
              ? `A fatura de ${mesExtenso} do ${nomeDoCartao} volta a aceitar gastos e o total deixa de ficar congelado.`
              : `A fatura de ${mesExtenso} do ${nomeDoCartao} volta a ficar fechada, sem data de pagamento.`
          }
          rotuloConfirmar={ROTULO_DA_ACAO[confirmando]}
          ocupado={mudar.isPending}
          aoCancelar={() => {
            mudar.reset()
            setConfirmando(null)
          }}
          aoConfirmar={() => void aplicar(confirmando)}
        />
      )}

      {!pagando && !confirmando && (principal || volta) && (
        <div className="flex flex-wrap gap-3">
          {principal && (
            <button
              type="button"
              disabled={mudar.isPending}
              onClick={() => {
                mudar.reset()
                if (principal === 'pagar') {
                  setPagaEm(hoje)
                  setPagando(true)
                } else {
                  void aplicar(principal)
                }
              }}
              className="h-11 flex-1 whitespace-nowrap rounded-xl bg-primary px-4 text-[15px] font-semibold text-primary-foreground disabled:opacity-60"
            >
              {mudar.isPending ? 'Salvando…' : ROTULO_DA_ACAO[principal]}
            </button>
          )}
          {volta && (
            <button
              type="button"
              onClick={() => {
                mudar.reset()
                setConfirmando(volta)
              }}
              className="h-11 flex-1 whitespace-nowrap rounded-xl border border-border px-4 text-[15px]"
            >
              {ROTULO_DA_ACAO[volta]}
            </button>
          )}
        </div>
      )}
    </div>
  )
}
