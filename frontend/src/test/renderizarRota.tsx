import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryHistory, RouterProvider } from '@tanstack/react-router'
import { render } from '@testing-library/react'
import { criarRouter } from '@/app/router'

/** Renderiza o app inteiro numa URL, com router em memória e cache limpo. */
export function renderizarRota(url: string) {
  const router = criarRouter(createMemoryHistory({ initialEntries: [url] }))
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const resultado = render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { ...resultado, router }
}
