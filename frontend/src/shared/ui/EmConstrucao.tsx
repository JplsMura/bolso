type Props = { titulo: string; descricao: string }

export function EmConstrucao({ titulo, descricao }: Props) {
  return (
    <section className="flex flex-col gap-2 rounded-2xl border border-border bg-card p-6">
      <h2 className="text-lg font-semibold">{titulo}</h2>
      <p className="text-sm text-muted-foreground">{descricao}</p>
    </section>
  )
}
