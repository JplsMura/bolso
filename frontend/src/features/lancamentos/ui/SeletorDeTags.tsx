import { useId, useState, type KeyboardEvent } from 'react'
import { cn } from '@/shared/lib/cn'
import { useCriarTag, useTags } from '../api/consultas'
import { mensagemDoErro } from '../api/erro'
import { MAXIMO_TAGS } from '../model/formulario'

type Props = {
  espacoId: string
  selecionadas: readonly string[]
  aoMudar: (ids: string[]) => void
  erro?: string | undefined
}

type Opcao = { tipo: 'tag'; id: string; nome: string; marcada: boolean } | { tipo: 'criar'; nome: string }

/**
 * Um campo só para as tags: digitar filtra as existentes e oferece criar uma nova; as escolhidas ficam embaixo,
 * cada uma com o botão de tirar. Combobox com lista (setas, Enter e Esc), no padrão do WAI-ARIA.
 */
export function SeletorDeTags({ espacoId, selecionadas, aoMudar, erro }: Props) {
  const base = useId()
  const idCampo = `${base}-campo`
  const idLista = `${base}-lista`
  const tags = useTags(espacoId)
  const criar = useCriarTag(espacoId)
  const [texto, setTexto] = useState('')
  const [aberto, setAberto] = useState(false)
  const [ativa, setAtiva] = useState(0)

  const busca = texto.trim().toLocaleLowerCase('pt-BR')
  const todas = tags.data ?? []
  const lotado = selecionadas.length >= MAXIMO_TAGS
  const opcoes: Opcao[] = todas
    .filter((t) => t.nome.toLocaleLowerCase('pt-BR').includes(busca))
    .map((t) => ({ tipo: 'tag', id: t.id, nome: t.nome, marcada: selecionadas.includes(t.id) }))
  const existeIgual = todas.some((t) => t.nome.toLocaleLowerCase('pt-BR') === busca)
  if (busca !== '' && !existeIgual) opcoes.push({ tipo: 'criar', nome: texto.trim() })
  const indiceAtivo = Math.min(ativa, Math.max(opcoes.length - 1, 0))
  const nomePorId = new Map(todas.map((t) => [t.id, t.nome]))

  function alternar(id: string) {
    if (selecionadas.includes(id)) aoMudar(selecionadas.filter((t) => t !== id))
    else if (!lotado) aoMudar([...selecionadas, id])
  }

  async function escolher(opcao: Opcao | undefined) {
    if (!opcao) return
    if (opcao.tipo === 'tag') {
      alternar(opcao.id)
      setTexto('')
      return
    }
    if (lotado || criar.isPending) return
    try {
      const nova = await criar.mutateAsync(opcao.nome)
      aoMudar([...selecionadas, nova.id])
      setTexto('')
    } catch {
      // a mensagem aparece abaixo, a partir de criar.error
    }
  }

  function teclas(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setAberto(true)
      setAtiva(Math.min(indiceAtivo + 1, opcoes.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setAtiva(Math.max(indiceAtivo - 1, 0))
    } else if (e.key === 'Enter') {
      // Enter aqui escolhe a tag, não envia o lançamento
      e.preventDefault()
      if (aberto) void escolher(opcoes[indiceAtivo])
    } else if (e.key === 'Escape') {
      setAberto(false)
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={idCampo} className="text-[13px] text-muted-foreground">
        Tags
      </label>
      <div className="relative">
        <input
          id={idCampo}
          role="combobox"
          aria-expanded={aberto}
          aria-controls={idLista}
          aria-autocomplete="list"
          aria-activedescendant={aberto && opcoes.length > 0 ? `${idLista}-${indiceAtivo}` : undefined}
          aria-invalid={erro ? true : undefined}
          autoComplete="off"
          maxLength={40}
          value={texto}
          placeholder={lotado ? `Máximo de ${MAXIMO_TAGS} tags` : 'Buscar ou criar tag'}
          onFocus={() => setAberto(true)}
          onBlur={() => setAberto(false)}
          onChange={(e) => {
            setTexto(e.target.value)
            setAtiva(0)
            setAberto(true)
            if (criar.isError) criar.reset()
          }}
          onKeyDown={teclas}
          className="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-[15px] placeholder:text-muted-foreground"
        />
        {aberto && (
          <ul
            id={idLista}
            role="listbox"
            aria-label="Tags"
            className="absolute inset-x-0 top-full z-20 mt-1.5 max-h-60 overflow-y-auto rounded-xl border border-border bg-popover p-1.5 shadow-lg"
          >
            {opcoes.length === 0 && (
              <li role="presentation" className="px-3 py-2.5 text-sm text-muted-foreground">
                {tags.isPending ? 'Carregando tags…' : 'Nenhuma tag ainda. Digite para criar.'}
              </li>
            )}
            {opcoes.map((o, i) => (
              <li
                key={o.tipo === 'tag' ? o.id : 'criar'}
                id={`${idLista}-${i}`}
                role="option"
                aria-selected={o.tipo === 'tag' && o.marcada}
                // mousedown sem tirar o foco do campo, para a lista não fechar antes do clique
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => void escolher(o)}
                onMouseEnter={() => setAtiva(i)}
                className={cn(
                  'flex min-h-11 cursor-pointer items-center gap-2.5 rounded-lg px-3 text-[15px]',
                  i === indiceAtivo && 'bg-card',
                )}
              >
                {o.tipo === 'tag' ? (
                  <>
                    <span aria-hidden="true" className={cn('w-4 text-primary', !o.marcada && 'invisible')}>
                      ✓
                    </span>
                    {o.nome}
                  </>
                ) : (
                  <span className="text-primary">{criar.isPending ? 'Criando…' : `Criar “${o.nome}”`}</span>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      {selecionadas.length > 0 && (
        <ul aria-label="Tags escolhidas" className="flex flex-wrap gap-2">
          {selecionadas.map((id) => {
            const nome = nomePorId.get(id) ?? 'Tag'
            return (
              <li
                key={id}
                className="inline-flex h-9 items-center gap-1 rounded-full border border-primary/50 bg-primary/10 pl-3.5 text-sm"
              >
                {nome}
                <button
                  type="button"
                  aria-label={`Remover a tag ${nome}`}
                  onClick={() => alternar(id)}
                  className="inline-flex size-9 items-center justify-center rounded-full text-muted-foreground hover:text-foreground"
                >
                  <span aria-hidden="true">×</span>
                </button>
              </li>
            )
          })}
        </ul>
      )}

      {criar.isError && (
        <p role="alert" className="text-sm text-destructive">
          {mensagemDoErro(criar.error, 'Não foi possível criar a tag.')}
        </p>
      )}
      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
    </div>
  )
}
