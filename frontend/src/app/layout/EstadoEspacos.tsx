/** Enquanto a API não devolveu os espaços. */
export function CarregandoEspacos() {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center p-6 text-center">
      <p role="status" className="text-[15px] text-muted-foreground">
        Carregando seus espaços…
      </p>
    </main>
  )
}

type ErroProps = { onTentarDeNovo: () => void; tentando?: boolean }

/** A API não respondeu, ou respondeu sem o espaço da URL. */
export function ErroEspacos({ onTentarDeNovo, tentando = false }: ErroProps) {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-2xl font-bold">Não foi possível carregar os espaços</h1>
      <p role="alert" className="max-w-md text-[15px] text-muted-foreground">
        Confira se a API está no ar e tente de novo.
      </p>
      <button
        type="button"
        onClick={onTentarDeNovo}
        disabled={tentando}
        className="h-12 rounded-xl bg-primary px-5 text-[15px] font-semibold text-primary-foreground disabled:opacity-60"
      >
        {tentando ? 'Tentando…' : 'Tentar de novo'}
      </button>
    </main>
  )
}
