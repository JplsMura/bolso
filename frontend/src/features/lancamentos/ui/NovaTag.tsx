import { useState } from 'react'
import { mensagemDoErro } from '../api/erro'
import { useCriarTag } from '../api/consultas'
import type { TagApi } from '../model/tipos'

type Props = { espacoId: string; aoCriar: (tag: TagApi) => void }

/** Cria uma tag sem sair do formulário. Enter aqui adiciona a tag, não envia o lançamento. */
export function NovaTag({ espacoId, aoCriar }: Props) {
  const [nome, setNome] = useState('')
  const criar = useCriarTag(espacoId)

  async function adicionar() {
    if (nome.trim() === '' || criar.isPending) return
    try {
      aoCriar(await criar.mutateAsync(nome.trim()))
      setNome('')
    } catch {
      // a mensagem aparece abaixo, a partir de criar.error
    }
  }

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex gap-2">
        <label htmlFor="nova-tag" className="sr-only">
          Nova tag
        </label>
        <input
          id="nova-tag"
          value={nome}
          maxLength={40}
          placeholder="Nova tag"
          onChange={(e) => {
            setNome(e.target.value)
            if (criar.isError) criar.reset()
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              void adicionar()
            }
          }}
          className="h-10 min-w-0 flex-1 rounded-xl border border-border bg-card px-3.5 text-sm placeholder:text-muted-foreground"
        />
        <button
          type="button"
          onClick={() => void adicionar()}
          disabled={criar.isPending || nome.trim() === ''}
          className="h-10 rounded-xl border border-border px-4 text-sm disabled:opacity-60"
        >
          Adicionar tag
        </button>
      </div>
      {criar.isError && (
        <p role="alert" className="text-sm text-destructive">
          {mensagemDoErro(criar.error, 'Não foi possível criar a tag.')}
        </p>
      )}
    </div>
  )
}
