import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, type ReactNode } from 'react'
import { type FieldPath, type FieldPathValue, useForm } from 'react-hook-form'
import { cn } from '@/shared/lib/cn'
import { centavosDeDigitos, formatarBRL } from '@/shared/lib/money'
import type { TipoEspaco } from '@/shared/lib/espacoAtual'
import { useMetas, useTags } from '../api/consultas'
import type { Direcao } from '../model/tipos'
import { criarSchema, type ValoresDoFormulario } from '../model/formulario'
import { FORMAS_OFERECIDAS } from '../model/pagamentos'
import { Chip } from './Chip'
import { NovaTag } from './NovaTag'

type Props = {
  espacoId: string
  tipoEspaco: TipoEspaco
  valoresIniciais: ValoresDoFormulario
  rotuloEnviar: string
  enviando: boolean
  aoEnviar: (valores: ValoresDoFormulario) => Promise<void>
  /** Erro vindo do servidor (422, falha de rede...), mostrado acima do botão. */
  erro?: string | undefined
  /** Fica ao lado do botão de enviar (por exemplo, "Excluir"). */
  acoesExtras?: ReactNode
}

const rotulo = 'text-[13px] text-muted-foreground'
const campo = 'h-11 w-full rounded-xl border border-border bg-card px-3.5 text-[15px]'

export function FormularioDeLancamento({
  espacoId,
  tipoEspaco,
  valoresIniciais,
  rotuloEnviar,
  enviando,
  aoEnviar,
  erro,
  acoesExtras,
}: Props) {
  const exigeMeta = tipoEspaco === 'HOME'
  const schema = useMemo(() => criarSchema(exigeMeta), [exigeMeta])
  const { handleSubmit, register, setValue, watch, formState } = useForm<ValoresDoFormulario>({
    resolver: zodResolver(schema),
    defaultValues: valoresIniciais,
  })
  const { errors, isSubmitted } = formState
  // O React Compiler não está ligado neste projeto, então o aviso de memoização do watch() não se aplica.
  // eslint-disable-next-line react-hooks/incompatible-library
  const valores = watch()
  const saida = valores.direcao === 'OUT'

  const metas = useMetas(espacoId, exigeMeta)
  const tags = useTags(espacoId)

  const mudar = <K extends FieldPath<ValoresDoFormulario>>(
    nome: K,
    valor: FieldPathValue<ValoresDoFormulario, K>,
  ) => setValue(nome, valor, { shouldDirty: true, shouldValidate: isSubmitted })

  function trocarDirecao(direcao: Direcao) {
    if (direcao === valores.direcao) return
    mudar('direcao', direcao)
    if (direcao === 'IN') {
      mudar('metaId', null)
      mudar('formaPagamento', null)
    }
  }

  function alternarTag(id: string) {
    mudar(
      'tagIds',
      valores.tagIds.includes(id) ? valores.tagIds.filter((t) => t !== id) : [...valores.tagIds, id],
    )
  }

  return (
    <form onSubmit={(e) => void handleSubmit(aoEnviar)(e)} noValidate className="flex flex-col gap-5">
      <div
        role="group"
        aria-label="Tipo de lançamento"
        className="flex gap-1 rounded-xl border border-border bg-card p-1"
      >
        <Opcao marcado={saida} aoEscolher={() => trocarDirecao('OUT')}>
          Gasto
        </Opcao>
        <Opcao marcado={!saida} aoEscolher={() => trocarDirecao('IN')}>
          Entrada
        </Opcao>
      </div>

      <div className="flex flex-col items-center gap-1">
        <label htmlFor="valor" className={rotulo}>
          Valor
        </label>
        <input
          id="valor"
          inputMode="numeric"
          autoComplete="off"
          value={formatarBRL(valores.centavos)}
          onChange={(e) => mudar('centavos', centavosDeDigitos(e.target.value))}
          aria-invalid={errors.centavos ? true : undefined}
          aria-describedby={errors.centavos ? 'erro-valor' : undefined}
          className="w-full bg-transparent text-center text-[40px] font-bold tracking-tight tabular-nums"
        />
        <Erro id="erro-valor" mensagem={errors.centavos?.message} />
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="descricao" className={rotulo}>
          Descrição
        </label>
        <input
          id="descricao"
          autoComplete="off"
          aria-invalid={errors.descricao ? true : undefined}
          aria-describedby={errors.descricao ? 'erro-descricao' : undefined}
          className={campo}
          {...register('descricao')}
        />
        <Erro id="erro-descricao" mensagem={errors.descricao?.message} />
      </div>

      {saida && exigeMeta && (
        <fieldset className="flex flex-col gap-1.5">
          <legend className={rotulo}>Meta</legend>
          {metas.isPending && (
            <p role="status" className="text-sm text-muted-foreground">
              Carregando metas…
            </p>
          )}
          {metas.isError && (
            <p role="alert" className="text-sm text-destructive">
              Não foi possível carregar as metas.{' '}
              <button type="button" className="underline" onClick={() => void metas.refetch()}>
                Tentar de novo
              </button>
            </p>
          )}
          <div className="flex flex-wrap gap-2">
            {metas.data?.map((m) => (
              <Chip
                key={m.id}
                cor={m.cor}
                marcado={valores.metaId === m.id}
                aoAlternar={() => mudar('metaId', m.id)}
              >
                {m.nome}
              </Chip>
            ))}
          </div>
          <Erro id="erro-meta" mensagem={errors.metaId?.message} />
        </fieldset>
      )}

      <fieldset className="flex flex-col gap-1.5">
        <legend className={rotulo}>Tag</legend>
        {tags.isError && (
          <p role="alert" className="text-sm text-destructive">
            Não foi possível carregar as tags.{' '}
            <button type="button" className="underline" onClick={() => void tags.refetch()}>
              Tentar de novo
            </button>
          </p>
        )}
        <div className="flex flex-wrap gap-2">
          {tags.data?.map((t) => (
            <Chip key={t.id} marcado={valores.tagIds.includes(t.id)} aoAlternar={() => alternarTag(t.id)}>
              {t.nome}
            </Chip>
          ))}
        </div>
        <NovaTag espacoId={espacoId} aoCriar={(tag) => mudar('tagIds', [...valores.tagIds, tag.id])} />
        <Erro id="erro-tags" mensagem={errors.tagIds?.message} />
      </fieldset>

      {saida && (
        <fieldset className="flex flex-col gap-1.5">
          <legend className={rotulo}>Forma de pagamento</legend>
          <div className="flex flex-wrap gap-1 rounded-xl border border-border bg-card p-1">
            {FORMAS_OFERECIDAS.map((f) => (
              <Opcao
                key={f.valor}
                marcado={valores.formaPagamento === f.valor}
                aoEscolher={() => mudar('formaPagamento', f.valor)}
              >
                {f.rotulo}
              </Opcao>
            ))}
          </div>
          <Erro id="erro-pagamento" mensagem={errors.formaPagamento?.message} />
        </fieldset>
      )}

      <div className="flex flex-col gap-1.5">
        <label htmlFor="data" className={rotulo}>
          Data
        </label>
        <input
          id="data"
          type="date"
          aria-invalid={errors.data ? true : undefined}
          aria-describedby={errors.data ? 'erro-data' : undefined}
          className={campo}
          {...register('data')}
        />
        <Erro id="erro-data" mensagem={errors.data?.message} />
      </div>

      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={enviando}
          className="h-12 flex-1 rounded-xl bg-primary px-5 text-[15px] font-semibold text-primary-foreground disabled:opacity-60"
        >
          {enviando ? 'Salvando…' : rotuloEnviar}
        </button>
        {acoesExtras}
      </div>
    </form>
  )
}

function Opcao({
  marcado,
  aoEscolher,
  children,
}: {
  marcado: boolean
  aoEscolher: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      aria-pressed={marcado}
      onClick={aoEscolher}
      className={cn(
        'h-10 flex-1 rounded-lg px-3 text-sm',
        marcado
          ? 'bg-primary font-semibold text-primary-foreground'
          : 'text-muted-foreground hover:text-foreground',
      )}
    >
      {children}
    </button>
  )
}

function Erro({ id, mensagem }: { id: string; mensagem: string | undefined }) {
  if (!mensagem) return null
  return (
    <p id={id} role="alert" className="text-sm text-destructive">
      {mensagem}
    </p>
  )
}
