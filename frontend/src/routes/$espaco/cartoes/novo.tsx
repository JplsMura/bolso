import { createFileRoute } from '@tanstack/react-router'
import { NovoCartaoPage } from '@/features/cartoes'

export const Route = createFileRoute('/$espaco/cartoes/novo')({
  component: Novo,
})

function Novo() {
  const { espaco } = Route.useParams()
  return <NovoCartaoPage espaco={espaco} />
}
