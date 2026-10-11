import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

type Props = { marcado: boolean; aoAlternar: () => void; cor?: string; children: ReactNode }

/** Botão de escolha (meta, tag): `aria-pressed` diz se está marcado, sem depender só da cor. */
export function Chip({ marcado, aoAlternar, cor, children }: Props) {
  return (
    <button
      type="button"
      aria-pressed={marcado}
      onClick={aoAlternar}
      className={cn(
        'inline-flex h-10 items-center gap-2 rounded-full border px-3.5 text-sm',
        marcado
          ? 'border-primary bg-primary/15 font-semibold text-foreground'
          : 'border-border bg-card text-muted-foreground hover:text-foreground',
      )}
    >
      {cor && <span aria-hidden="true" className="size-2.5 rounded-full" style={{ backgroundColor: cor }} />}
      {children}
    </button>
  )
}
