import { createFileRoute, notFound } from '@tanstack/react-router'
import { ehEspaco } from '@/app/espacos'
import { AppShell } from '@/app/layout/AppShell'

/** O espaço vive no endereço: /casa/... ou /empresa/... */
export const Route = createFileRoute('/$espaco')({
  params: {
    parse: ({ espaco }) => {
      if (!ehEspaco(espaco)) throw notFound()
      return { espaco }
    },
  },
  component: Layout,
})

function Layout() {
  const { espaco } = Route.useParams()
  return <AppShell espaco={espaco} />
}
