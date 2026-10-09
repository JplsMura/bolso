import { createFileRoute } from '@tanstack/react-router'
import { ROTULO_ESPACO } from '@/app/espacos'
import { EmConstrucao } from '@/shared/ui/EmConstrucao'

export const Route = createFileRoute('/$espaco/')({
  component: Inicio,
})

function Inicio() {
  const { espaco } = Route.useParams()
  return (
    <>
      <header className="flex flex-col gap-1">
        <h1 className="text-[28px] font-bold tracking-tight">Início</h1>
        <p className="text-[15px] text-muted-foreground">{ROTULO_ESPACO[espaco]}</p>
      </header>
      <EmConstrucao
        titulo="Em construção"
        descricao={
          espaco === 'casa'
            ? 'As metas do mês, os gastos recentes e os cartões chegam com as features de Lançamentos, Cartões e Orçamento.'
            : 'O painel do teto do MEI e as notas chegam com a feature de Faturamento PJ.'
        }
      />
    </>
  )
}
