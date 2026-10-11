import { useEffect, useRef } from 'react'

type Props = {
  descricao: string
  excluindo: boolean
  erro?: string | undefined
  aoCancelar: () => void
  aoConfirmar: () => void
}

/** Pergunta antes de excluir. O foco começa no botão seguro (Cancelar) e o Esc cancela. */
export function ConfirmarExclusao({ descricao, excluindo, erro, aoCancelar, aoConfirmar }: Props) {
  const cancelar = useRef<HTMLButtonElement>(null)
  useEffect(() => cancelar.current?.focus(), [])

  return (
    <div
      role="alertdialog"
      aria-labelledby="titulo-exclusao"
      aria-describedby="texto-exclusao"
      onKeyDown={(e) => {
        if (e.key === 'Escape') aoCancelar()
      }}
      className="flex flex-col gap-3 rounded-2xl border border-destructive/60 bg-card p-5"
    >
      <h2 id="titulo-exclusao" className="text-lg font-semibold">
        Excluir este lançamento?
      </h2>
      <p id="texto-exclusao" className="text-sm text-muted-foreground">
        “{descricao}” sai da lista e dos totais do mês. Não dá para desfazer por aqui.
      </p>
      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
      <div className="flex gap-3">
        <button
          ref={cancelar}
          type="button"
          onClick={aoCancelar}
          disabled={excluindo}
          className="h-11 flex-1 rounded-xl border border-border px-5 text-[15px]"
        >
          Cancelar
        </button>
        <button
          type="button"
          onClick={aoConfirmar}
          disabled={excluindo}
          className="h-11 flex-1 rounded-xl bg-destructive px-5 text-[15px] font-semibold text-background disabled:opacity-60"
        >
          {excluindo ? 'Excluindo…' : 'Excluir'}
        </button>
      </div>
    </div>
  )
}
