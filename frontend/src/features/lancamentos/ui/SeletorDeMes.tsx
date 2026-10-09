import { rotuloCurtoDoMes, somarMeses } from '../model/mes'

type Props = { mes: string; aoMudar: (mes: string) => void }

const botao =
  'inline-flex size-11 items-center justify-center rounded-full text-lg text-muted-foreground hover:bg-popover hover:text-foreground'

export function SeletorDeMes({ mes, aoMudar }: Props) {
  return (
    <div role="group" aria-label="Mês" className="flex items-center">
      <button
        type="button"
        aria-label="Mês anterior"
        className={botao}
        onClick={() => aoMudar(somarMeses(mes, -1))}
      >
        <span aria-hidden="true">‹</span>
      </button>
      <span aria-live="polite" className="min-w-20 text-center text-sm text-muted-foreground">
        {rotuloCurtoDoMes(mes)}
      </span>
      <button
        type="button"
        aria-label="Próximo mês"
        className={botao}
        onClick={() => aoMudar(somarMeses(mes, 1))}
      >
        <span aria-hidden="true">›</span>
      </button>
    </div>
  )
}
