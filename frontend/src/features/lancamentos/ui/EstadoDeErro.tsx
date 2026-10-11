type Props = { mensagem: string; aoTentarDeNovo: () => void; tentando?: boolean }

/** Falha ao carregar: o alerta e o botão para repetir a consulta. */
export function EstadoDeErro({ mensagem, aoTentarDeNovo, tentando = false }: Props) {
  return (
    <div className="flex flex-col items-start gap-3 rounded-2xl border border-border bg-card p-5">
      <p role="alert" className="text-[15px]">
        {mensagem}
      </p>
      <button
        type="button"
        onClick={aoTentarDeNovo}
        disabled={tentando}
        className="h-11 rounded-xl bg-primary px-5 text-[15px] font-semibold text-primary-foreground disabled:opacity-60"
      >
        {tentando ? 'Tentando…' : 'Tentar de novo'}
      </button>
    </div>
  )
}
