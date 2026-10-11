import { useEffect, useRef } from 'react'

type Props = {
  titulo: string
  texto: string
  rotuloConfirmar: string
  ocupado: boolean
  erro?: string | undefined
  perigo?: boolean
  aoCancelar: () => void
  aoConfirmar: () => void
}

/** Pergunta antes de uma ação de volta (reabrir, desfazer pagamento, arquivar). Foco no Cancelar; Esc cancela. */
export function Confirmacao({
  titulo,
  texto,
  rotuloConfirmar,
  ocupado,
  erro,
  perigo = false,
  aoCancelar,
  aoConfirmar,
}: Props) {
  const cancelar = useRef<HTMLButtonElement>(null)
  useEffect(() => cancelar.current?.focus(), [])

  return (
    <div
      role="alertdialog"
      aria-labelledby="titulo-confirmacao"
      aria-describedby="texto-confirmacao"
      onKeyDown={(e) => {
        if (e.key === 'Escape') aoCancelar()
      }}
      className="flex flex-col gap-3 rounded-2xl border border-warning/60 bg-background p-4"
    >
      <h2 id="titulo-confirmacao" className="text-base font-semibold">
        {titulo}
      </h2>
      <p id="texto-confirmacao" className="text-sm text-muted-foreground">
        {texto}
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
          disabled={ocupado}
          className="h-11 flex-1 rounded-xl border border-border px-4 text-[15px]"
        >
          Cancelar
        </button>
        <button
          type="button"
          onClick={aoConfirmar}
          disabled={ocupado}
          className={
            perigo
              ? 'h-11 flex-1 rounded-xl bg-destructive px-4 text-[15px] font-semibold text-background disabled:opacity-60'
              : 'h-11 flex-1 rounded-xl bg-primary px-4 text-[15px] font-semibold text-primary-foreground disabled:opacity-60'
          }
        >
          {ocupado ? 'Aguarde…' : rotuloConfirmar}
        </button>
      </div>
    </div>
  )
}
