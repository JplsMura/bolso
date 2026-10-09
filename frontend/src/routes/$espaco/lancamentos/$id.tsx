import { createFileRoute } from '@tanstack/react-router'
import { EditarLancamentoPage } from '@/features/lancamentos'

export const Route = createFileRoute('/$espaco/lancamentos/$id')({
  component: Editar,
})

function Editar() {
  const { espaco, id } = Route.useParams()
  return <EditarLancamentoPage espaco={espaco} id={id} />
}
