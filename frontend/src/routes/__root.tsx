import { createRootRoute, Link, Outlet } from '@tanstack/react-router'

export const Route = createRootRoute({
  component: Outlet,
  notFoundComponent: NaoEncontrada,
})

function NaoEncontrada() {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-2xl font-bold">Página não encontrada</h1>
      <Link to="/$espaco" params={{ espaco: 'casa' }} className="font-semibold text-primary">
        Voltar para o início
      </Link>
    </main>
  )
}
