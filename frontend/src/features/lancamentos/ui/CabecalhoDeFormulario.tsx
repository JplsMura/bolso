import { Link } from '@tanstack/react-router'
import type { SlugDoEspaco } from '@/shared/lib/espacoAtual'

type Props = { titulo: string; espaco: SlugDoEspaco; mes?: string | undefined }

/** Título com o botão de fechar, que volta para a lista (no mês do lançamento, quando se sabe). */
export function CabecalhoDeFormulario({ titulo, espaco, mes }: Props) {
  return (
    <header className="flex items-center justify-between">
      <Link
        to="/$espaco/lancamentos"
        params={{ espaco }}
        search={mes ? { mes } : {}}
        aria-label="Fechar"
        className="inline-flex size-11 items-center justify-center rounded-full text-xl text-muted-foreground hover:bg-popover hover:text-foreground"
      >
        <span aria-hidden="true">×</span>
      </Link>
      <h1 className="text-lg font-bold">{titulo}</h1>
      <span className="w-11" aria-hidden="true" />
    </header>
  )
}
