import { Link } from '@tanstack/react-router'
import type { SlugDoEspaco } from '@/shared/lib/espacoAtual'

/** Título com o botão de fechar, que volta para Cartões. */
export function CabecalhoDoCartao({ titulo, espaco }: { titulo: string; espaco: SlugDoEspaco }) {
  return (
    <header className="flex items-center justify-between">
      <Link
        to="/$espaco/cartoes"
        params={{ espaco }}
        search={{}}
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
