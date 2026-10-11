import { createFileRoute } from '@tanstack/react-router'
import { ehMes, ListaDeLancamentosPage } from '@/features/lancamentos'

/** /casa/lancamentos?mes=2026-03 — sem `mes`, vale o mês atual. */
export const Route = createFileRoute('/$espaco/lancamentos/')({
  validateSearch: (busca: Record<string, unknown>): { mes?: string } =>
    typeof busca.mes === 'string' && ehMes(busca.mes) ? { mes: busca.mes } : {},
  component: Lista,
})

function Lista() {
  const { espaco } = Route.useParams()
  const { mes } = Route.useSearch()
  // key: ao trocar de espaço a lista começa do zero (filtros, busca e metas não vazam da Casa para a Empresa)
  return <ListaDeLancamentosPage key={espaco} espaco={espaco} mes={mes} />
}
