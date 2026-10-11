import { createFileRoute } from '@tanstack/react-router'
import { CartoesPage } from '@/features/cartoes'
import { ehMes } from '@/features/lancamentos'

/** /casa/cartoes?mes=2026-10 — sem `mes`, vale o mês atual. */
export const Route = createFileRoute('/$espaco/cartoes/')({
  validateSearch: (busca: Record<string, unknown>): { mes?: string } =>
    typeof busca.mes === 'string' && ehMes(busca.mes) ? { mes: busca.mes } : {},
  component: Cartoes,
})

function Cartoes() {
  const { espaco } = Route.useParams()
  const { mes } = Route.useSearch()
  return <CartoesPage key={espaco} espaco={espaco} mes={mes} />
}
