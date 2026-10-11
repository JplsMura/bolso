import { createFileRoute } from '@tanstack/react-router'
import { EditarCartaoPage } from '@/features/cartoes'

export const Route = createFileRoute('/$espaco/cartoes/$id')({
  component: Editar,
})

function Editar() {
  const { espaco, id } = Route.useParams()
  return <EditarCartaoPage espaco={espaco} id={id} />
}
