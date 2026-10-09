import { createFileRoute } from '@tanstack/react-router'
import { NovoLancamentoPage } from '@/features/lancamentos'

/** /casa/lancamentos/novo — com ?tipo=entrada, abre já como entrada. */
export const Route = createFileRoute('/$espaco/lancamentos/novo')({
  validateSearch: (busca: Record<string, unknown>): { tipo?: 'entrada' } =>
    busca.tipo === 'entrada' ? { tipo: 'entrada' } : {},
  component: Novo,
})

function Novo() {
  const { espaco } = Route.useParams()
  const { tipo } = Route.useSearch()
  return <NovoLancamentoPage espaco={espaco} tipo={tipo} />
}
