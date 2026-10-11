import { Link } from '@tanstack/react-router'
import type { ReactNode } from 'react'
import { rotuloLongoDoMes } from '@/shared/lib/mes'
import { formatarBRL, paraCentavos } from '@/shared/lib/money'
import { useEspacoAtual } from '@/shared/lib/espacoAtual'
import { useCartoes, useFaturaDaCompra } from '../api/consultas'
import { cn } from '@/shared/lib/cn'
import { Chip } from './Chip'

type Props = {
  espacoId: string
  cartaoId: string | null
  aoEscolher: (id: string) => void
  data: string
  /** Valor digitado (o total, se for parcelado): é o que vai ocupar o limite. */
  centavos: number
  /** Parte desse valor que já ocupa o limite (edição de um gasto que já estava neste cartão). */
  centavosJaNoCartao: number
  erro: string | undefined
  /** Fica no bloco do cartão escolhido (as parcelas). */
  children?: ReactNode
}

const rotulo = 'text-[13px] text-muted-foreground'

/** dd/MM a partir de "2026-11-25" */
const diaEMes = (data: string) => `${data.slice(8, 10)}/${data.slice(5, 7)}`

/** Os cartões com o disponível, o aviso de em qual fatura a compra cai e o de limite estourado. */
export function EscolhaDoCartao({
  espacoId,
  cartaoId,
  aoEscolher,
  data,
  centavos,
  centavosJaNoCartao,
  erro,
  children,
}: Props) {
  const { tipo } = useEspacoAtual()
  const cartoes = useCartoes(espacoId)
  const fatura = useFaturaDaCompra(espacoId, cartaoId, data)
  const escolhido = cartoes.data?.find((c) => c.id === cartaoId)
  const disponivel = escolhido ? paraCentavos(escolhido.disponivel) + centavosJaNoCartao : 0
  const estoura = escolhido !== undefined && centavos > 0 && centavos > disponivel

  return (
    <fieldset className="flex min-w-0 flex-col gap-2">
      <legend className={cn(rotulo, 'mb-2')}>Cartão</legend>
      {cartoes.isPending && (
        <p role="status" className="text-sm text-muted-foreground">
          Carregando cartões…
        </p>
      )}
      {cartoes.isError && (
        <p role="alert" className="text-sm text-destructive">
          Não foi possível carregar os cartões.{' '}
          <button type="button" className="underline" onClick={() => void cartoes.refetch()}>
            Tentar de novo
          </button>
        </p>
      )}
      {cartoes.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Nenhum cartão cadastrado.{' '}
          <Link
            to="/$espaco/cartoes/novo"
            params={{ espaco: tipo === 'HOME' ? 'casa' : 'empresa' }}
            className="text-primary underline"
          >
            Cadastrar cartão
          </Link>
        </p>
      )}
      <div className="flex flex-col gap-3 md:grid md:grid-cols-2 md:items-start md:gap-6">
        <div className="flex min-w-0 flex-col gap-2">
          <div className="flex gap-2.5 overflow-x-auto pb-1 md:flex-wrap md:overflow-visible md:pb-0 [&>*]:shrink-0">
            {cartoes.data?.map((c) => (
              <Chip key={c.id} marcado={cartaoId === c.id} aoAlternar={() => aoEscolher(c.id)}>
                {c.nome}
              </Chip>
            ))}
          </div>
          {erro && (
            <p id="erro-cartao" role="alert" className="text-sm text-destructive">
              {erro}
            </p>
          )}
          {escolhido && children && <div className="mt-3">{children}</div>}
        </div>
        {escolhido && (
          <div className="flex flex-col gap-3 rounded-xl bg-background p-4">
            <p className="text-sm">
              <span className="text-muted-foreground">Disponível no {escolhido.nome}: </span>
              <span className="font-semibold tabular-nums">{formatarBRL(disponivel)}</span>
            </p>
            {fatura.data && (
              <p aria-live="polite" className="text-sm text-muted-foreground">
                {fatura.data.status === 'CLOSED' || fatura.data.status === 'PAID' ? (
                  <span className="text-warning">
                    A fatura de {rotuloLongoDoMes(fatura.data.mes)} do {escolhido.nome} já está{' '}
                    {fatura.data.status === 'PAID' ? 'paga' : 'fechada'}. Reabra a fatura em Cartões para
                    lançar nela.
                  </span>
                ) : (
                  <>
                    Entra na fatura de {rotuloLongoDoMes(fatura.data.mes)} do {escolhido.nome} · fecha{' '}
                    {diaEMes(fatura.data.fechamento)} · vence {diaEMes(fatura.data.vencimento)}.
                  </>
                )}
              </p>
            )}
            {estoura && (
              <p className="text-sm text-warning">
                Passa do limite do {escolhido.nome}: disponível {formatarBRL(disponivel)}. Dá para salvar
                mesmo assim.
              </p>
            )}
          </div>
        )}
      </div>
    </fieldset>
  )
}
