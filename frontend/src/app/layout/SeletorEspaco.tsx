import { Link } from '@tanstack/react-router'
import { useId, useRef, useState } from 'react'
import { cn } from '@/shared/lib/cn'
import { ESPACOS, type Espaco } from '../espacos'

type Props = { espaco: Espaco; nomes: Record<Espaco, string>; className?: string }

/** Botão que abre a lista de espaços. Trocar de espaço é trocar de URL; o nome vem da API. */
export function SeletorEspaco({ espaco, nomes, className }: Props) {
  const [aberto, setAberto] = useState(false)
  const listaId = useId()
  const botao = useRef<HTMLButtonElement>(null)

  function fechar() {
    setAberto(false)
    botao.current?.focus()
  }

  return (
    <div
      className={cn('relative', className)}
      onKeyDown={(e) => {
        if (e.key === 'Escape' && aberto) fechar()
      }}
      onBlur={(e) => {
        // fecha ao clicar ou tabular para fora do seletor
        if (!e.currentTarget.contains(e.relatedTarget)) setAberto(false)
      }}
    >
      <button
        ref={botao}
        type="button"
        aria-label={`Trocar de espaço, ${nomes[espaco]}`}
        aria-expanded={aberto}
        aria-controls={listaId}
        onClick={() => setAberto((v) => !v)}
        className="flex h-12 w-full items-center gap-2.5 rounded-xl border border-border bg-card px-3.5 text-left text-[15px] font-semibold"
      >
        <IconeEspaco espaco={espaco} />
        <span className="flex-1">{nomes[espaco]}</span>
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          className="text-muted-foreground"
          aria-hidden="true"
        >
          <path d="M6 9l6 6 6-6" />
        </svg>
      </button>

      {aberto && (
        <ul
          id={listaId}
          className="absolute inset-x-0 top-full z-20 mt-2 flex flex-col gap-1 rounded-xl border border-border bg-popover p-1.5"
        >
          {ESPACOS.map((e) => (
            <li key={e}>
              <Link
                to="/$espaco"
                params={{ espaco: e }}
                onClick={() => setAberto(false)}
                aria-current={e === espaco ? 'page' : undefined}
                className={cn(
                  'flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-[15px]',
                  e === espaco ? 'font-semibold text-primary' : 'text-foreground hover:bg-card',
                )}
              >
                <IconeEspaco espaco={e} />
                {nomes[e]}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function IconeEspaco({ espaco }: { espaco: Espaco }) {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="text-primary"
      aria-hidden="true"
    >
      {espaco === 'casa' ? (
        <>
          <path d="M3 11l9-8 9 8" />
          <path d="M5 10v10h14V10" />
        </>
      ) : (
        <>
          <rect x="3" y="7" width="18" height="13" rx="2" />
          <path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </>
      )}
    </svg>
  )
}
