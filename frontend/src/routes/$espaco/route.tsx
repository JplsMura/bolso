import { createFileRoute, notFound } from '@tanstack/react-router'
import { useMemo } from 'react'
import { acharEspaco, ehEspaco, nomesDosEspacos } from '@/app/espacos'
import { AppShell } from '@/app/layout/AppShell'
import { CarregandoEspacos, ErroEspacos } from '@/app/layout/EstadoEspacos'
import { useEspacos } from '@/features/identidade'
import { EspacoAtualContext } from '@/shared/lib/espacoAtual'

/** O espaço vive no endereço: /casa/... ou /empresa/... O id vem da API. */
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
  const espacos = useEspacos()
  const atual = espacos.data ? acharEspaco(espacos.data, espaco) : undefined
  const valor = useMemo(() => (atual ? { id: atual.id, tipo: atual.tipo, nome: atual.nome } : null), [atual])

  if (espacos.isPending) return <CarregandoEspacos />
  // com dados em cache, uma falha ao revalidar não derruba a tela em uso
  if ((espacos.isError && !espacos.data) || !espacos.data || !valor) {
    return <ErroEspacos tentando={espacos.isFetching} onTentarDeNovo={() => void espacos.refetch()} />
  }

  return (
    <EspacoAtualContext.Provider value={valor}>
      <AppShell espaco={espaco} nomes={nomesDosEspacos(espacos.data)} />
    </EspacoAtualContext.Provider>
  )
}
