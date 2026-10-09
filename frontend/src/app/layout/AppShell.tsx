import { Link, Outlet } from '@tanstack/react-router'
import { cn } from '@/shared/lib/cn'
import { type Espaco, type ItemMenu, MENU } from '../espacos'
import { SeletorEspaco } from './SeletorEspaco'

type Props = { espaco: Espaco; nomes: Record<Espaco, string> }

/**
 * Computador: menu lateral com o seletor de espaço.
 * Celular (referência de uso diário): seletor no topo e abas na barra inferior.
 */
export function AppShell({ espaco, nomes }: Props) {
  const menu = MENU[espaco]

  return (
    <div className="flex min-h-dvh">
      <nav
        aria-label="Principal"
        className="hidden w-64 shrink-0 flex-col gap-6 border-r border-border px-4 py-6 md:flex"
      >
        <SeletorEspaco espaco={espaco} nomes={nomes} />
        <ul className="flex flex-col gap-1">
          {menu.map((item) => (
            <li key={item.rotulo}>
              <ItemLateral espaco={espaco} item={item} />
            </li>
          ))}
        </ul>
      </nav>

      <div className="flex min-w-0 flex-1 flex-col">
        <div className="p-4 md:hidden">
          <SeletorEspaco espaco={espaco} nomes={nomes} className="w-fit min-w-40" />
        </div>
        <main className="flex flex-1 flex-col gap-6 px-4 pb-28 md:p-8">
          <Outlet />
        </main>
      </div>

      <nav
        aria-label="Atalhos"
        className="fixed inset-x-0 bottom-0 flex h-[76px] border-t border-border bg-card px-2 md:hidden"
      >
        {menu
          .filter((item) => item.noCelular)
          .map((item) => (
            <ItemInferior key={item.rotulo} espaco={espaco} item={item} />
          ))}
      </nav>
    </div>
  )
}

const baseLateral = 'block rounded-[10px] px-3.5 py-3 text-[15px]'

function ItemLateral({ espaco, item }: { espaco: Espaco; item: ItemMenu }) {
  if (!item.disponivel) {
    return (
      <span className={cn(baseLateral, 'text-muted-foreground/60')}>
        {item.rotulo}
        <span className="sr-only"> (em breve)</span>
      </span>
    )
  }
  return (
    <Link
      to="/$espaco"
      params={{ espaco }}
      activeOptions={{ exact: true }}
      className={cn(baseLateral, 'text-muted-foreground hover:text-foreground')}
      activeProps={{ className: 'bg-popover font-semibold text-primary hover:text-primary' }}
    >
      {item.rotulo}
    </Link>
  )
}

const baseInferior = 'flex flex-1 flex-col items-center justify-center gap-1 text-xs'

function ItemInferior({ espaco, item }: { espaco: Espaco; item: ItemMenu }) {
  if (!item.disponivel) {
    return (
      <span className={cn(baseInferior, 'text-muted-foreground/60')}>
        {item.rotulo}
        <span className="sr-only"> (em breve)</span>
      </span>
    )
  }
  return (
    <Link
      to="/$espaco"
      params={{ espaco }}
      activeOptions={{ exact: true }}
      className={cn(baseInferior, 'text-muted-foreground')}
      activeProps={{ className: 'font-semibold text-primary' }}
    >
      {item.rotulo}
    </Link>
  )
}
