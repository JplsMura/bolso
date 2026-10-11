import { createFileRoute } from '@tanstack/react-router'
import { ParcelamentoPage } from '@/features/lancamentos'

export const Route = createFileRoute('/$espaco/parcelamentos/$id')({
  component: Parcelamento,
})

function Parcelamento() {
  const { espaco, id } = Route.useParams()
  return <ParcelamentoPage espaco={espaco} id={id} />
}
