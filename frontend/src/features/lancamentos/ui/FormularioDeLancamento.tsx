import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from '@tanstack/react-router'
import { useMemo, useState, type ReactNode } from 'react'
import { type FieldErrors, type FieldPath, type FieldPathValue, useForm } from 'react-hook-form'
import { cn } from '@/shared/lib/cn'
import { centavosDeDigitos, formatarBRL } from '@/shared/lib/money'
import { MAXIMO_PARCELAS, resumoDasParcelas } from '../model/parcelas'
import type { TipoEspaco } from '@/shared/lib/espacoAtual'
import { useCartoes, useMetas } from '../api/consultas'
import type { Direcao } from '../model/tipos'
import { criarSchema, ehParcelado, type ValoresDoFormulario } from '../model/formulario'
import { FORMAS_OFERECIDAS } from '../model/pagamentos'
import { Chip } from './Chip'
import { EscolhaDoCartao } from './EscolhaDoCartao'
import { SeletorDeTags } from './SeletorDeTags'

type Props = {
  espacoId: string
  tipoEspaco: TipoEspaco
  valoresIniciais: ValoresDoFormulario
  rotuloEnviar: string
  enviando: boolean
  aoEnviar: (valores: ValoresDoFormulario) => Promise<void>
  /** Erro vindo do servidor (422, falha de rede...), mostrado acima do botão. */
  erro?: string | undefined
  /** Mostra o atalho para Cartões junto do erro (o gasto caiu numa fatura fechada). */
  erroDeFaturaFechada?: boolean
  /** Fica ao lado do botão de enviar (por exemplo, "Excluir"). */
  acoesExtras?: ReactNode
  /** Só no gasto novo: o campo Parcelas (no cartão). */
  permiteParcelar?: boolean
}

const rotulo = 'text-[13px] text-muted-foreground'
const campo = 'h-11 w-full rounded-xl border border-border bg-background px-3.5 text-[15px]'
const bloco = 'flex min-w-0 flex-col rounded-2xl border border-border bg-card p-5 md:p-6'
/** Chips numa linha só que rola para o lado no celular; no computador, quebram linha. */
const linhaDeChips =
  'flex gap-2.5 overflow-x-auto pb-1 md:flex-wrap md:overflow-visible md:pb-0 [&>*]:shrink-0'
const FORMAS_ESCONDIDAS: readonly string[] = ['TRANSFER', 'OTHER']

export function FormularioDeLancamento({
  espacoId,
  tipoEspaco,
  valoresIniciais,
  rotuloEnviar,
  enviando,
  aoEnviar,
  erro,
  erroDeFaturaFechada = false,
  acoesExtras,
  permiteParcelar = false,
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
      mudar('cartaoId', null)
      mudar('parcelas', 1)
    }
  }

  function trocarForma(forma: NonNullable<ValoresDoFormulario['formaPagamento']>) {
    mudar('formaPagamento', forma)
    if (forma !== 'CREDIT') {
      mudar('cartaoId', null)
      mudar('parcelas', 1)
    }
  }

  // na edição, o próprio gasto já ocupa o limite do cartão em que estava
  const jaNoCartao =
    valoresIniciais.formaPagamento === 'CREDIT' && valoresIniciais.cartaoId === valores.cartaoId
      ? valoresIniciais.centavos
      : 0

  const credito = saida && valores.formaPagamento === 'CREDIT'
  const cartoes = useCartoes(espacoId, credito)
  // Transferência e Outro ficam atrás de "Mais…", a não ser que uma delas já esteja escolhida
  const [mostrarTodas, setMostrarTodas] = useState(false)
  const formaEscondida = valores.formaPagamento !== null && FORMAS_ESCONDIDAS.includes(valores.formaPagamento)

  const resumo = [
    valores.centavos > 0 ? formatarBRL(valores.centavos) : undefined,
    credito ? cartoes.data?.find((c) => c.id === valores.cartaoId)?.nome : undefined,
    ehParcelado(valores) && valores.centavos >= valores.parcelas
      ? resumoDasParcelas(valores.centavos, valores.parcelas)
      : undefined,
    saida && exigeMeta ? metas.data?.find((m) => m.id === valores.metaId)?.nome : undefined,
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <form
      onSubmit={(e) => void handleSubmit(aoEnviar)(e)}
      noValidate
      className="flex flex-col gap-5 md:gap-6"
    >
      {/* faixa 1: o quê e quanto (no computador, valor, descrição e data numa linha) */}
      <section aria-label="O quê e quanto" className={cn(bloco, 'gap-5')}>
        <div
          role="group"
          aria-label="Tipo de lançamento"
          className="flex gap-1 rounded-xl border border-border bg-background p-1 md:max-w-sm"
        >
          <Opcao marcado={saida} aoEscolher={() => trocarDirecao('OUT')}>
            Gasto
          </Opcao>
          <Opcao marcado={!saida} aoEscolher={() => trocarDirecao('IN')}>
            Entrada
          </Opcao>
        </div>

        <div className="grid grid-cols-[minmax(0,1fr)_9.5rem] items-end gap-x-4 gap-y-5 md:grid-cols-[13rem_minmax(0,1fr)_10rem]">
          <div className="flex min-w-0 flex-col">
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
              className="w-full bg-transparent text-[28px] leading-[44px] font-bold tracking-tight tabular-nums sm:text-[32px]"
            />
          </div>
          <div className="order-3 col-span-2 flex flex-col gap-2 md:order-none md:col-span-1">
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
          </div>
          <div className="flex flex-col gap-2">
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
          </div>
        </div>
        <Erro id="erro-valor" mensagem={errors.centavos?.message} />
        <Erro id="erro-descricao" mensagem={errors.descricao?.message} />
        <Erro id="erro-data" mensagem={errors.data?.message} />
      </section>

      {/* faixa 2: como pagou */}
      {saida && (
        <section aria-label="Como pagou" className={cn(bloco, 'gap-6')}>
          <fieldset className="flex min-w-0 flex-col gap-2">
            <legend className={cn(rotulo, 'mb-2')}>Forma de pagamento</legend>
            <div className={linhaDeChips}>
              {FORMAS_OFERECIDAS.map((f) => {
                // no celular, Transferência e Outro ficam atrás de "Mais…"; no computador cabem todas
                const escondida = FORMAS_ESCONDIDAS.includes(f.valor) && !mostrarTodas && !formaEscondida
                return (
                  <Pilula
                    key={f.valor}
                    marcado={valores.formaPagamento === f.valor}
                    aoEscolher={() => trocarForma(f.valor)}
                    escondidaNoCelular={escondida}
                  >
                    {f.rotulo}
                  </Pilula>
                )
              })}
              {!mostrarTodas && !formaEscondida && (
                <button
                  type="button"
                  onClick={() => setMostrarTodas(true)}
                  className="h-10 shrink-0 rounded-full border border-dashed border-border px-3.5 text-sm text-muted-foreground hover:text-foreground md:hidden"
                >
                  Mais…
                </button>
              )}
            </div>
            <Erro id="erro-pagamento" mensagem={errors.formaPagamento?.message} />
          </fieldset>

          {credito && (
            <EscolhaDoCartao
              espacoId={espacoId}
              cartaoId={valores.cartaoId}
              aoEscolher={(id) => mudar('cartaoId', id)}
              data={valores.data}
              centavos={valores.centavos}
              centavosJaNoCartao={jaNoCartao}
              erro={errors.cartaoId?.message}
            >
              {permiteParcelar && <CamposDeParcelas valores={valores} mudar={mudar} erros={errors} />}
            </EscolhaDoCartao>
          )}
        </section>
      )}

      {/* faixa 3: meta e tags */}
      <section aria-label="Meta e tag" className={cn(bloco, 'gap-6')}>
        {saida && exigeMeta && (
          <fieldset className="flex min-w-0 flex-col gap-2">
            <legend className={cn(rotulo, 'mb-2')}>Meta</legend>
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
            <div className={linhaDeChips}>
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

        <div className="md:max-w-md">
          <SeletorDeTags
            espacoId={espacoId}
            selecionadas={valores.tagIds}
            aoMudar={(ids) => mudar('tagIds', ids)}
            erro={errors.tagIds?.message}
          />
        </div>
      </section>

      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
          {erroDeFaturaFechada && (
            <>
              {' '}
              <Link
                to="/$espaco/cartoes"
                params={{ espaco: tipoEspaco === 'HOME' ? 'casa' : 'empresa' }}
                className="underline"
              >
                Ir para Cartões
              </Link>
            </>
          )}
        </p>
      )}

      {/* rodapé fixo: no celular fica logo acima do menu de baixo (76 px); no computador, no pé da tela */}
      <div className="sticky bottom-[76px] z-10 -mx-4 flex items-center gap-3 border-t border-border bg-background px-4 py-3 md:bottom-0 md:mx-0 md:rounded-2xl md:border md:bg-card md:px-5">
        <p className="hidden min-w-0 flex-1 truncate text-[15px] text-muted-foreground md:block">{resumo}</p>
        {acoesExtras}
        <button
          type="submit"
          disabled={enviando}
          className="h-12 flex-1 whitespace-nowrap rounded-xl bg-primary px-6 text-[15px] font-semibold text-primary-foreground disabled:opacity-60 md:flex-none"
        >
          {enviando
            ? 'Salvando…'
            : ehParcelado(valores)
              ? `${rotuloEnviar} em ${valores.parcelas}x`
              : rotuloEnviar}
        </button>
      </div>
    </form>
  )
}

/** Forma de pagamento: pílula que não encolhe (a linha rola para o lado no celular). */
function Pilula({
  marcado,
  aoEscolher,
  escondidaNoCelular = false,
  children,
}: {
  marcado: boolean
  aoEscolher: () => void
  escondidaNoCelular?: boolean
  children: ReactNode
}) {
  return (
    <button
      type="button"
      aria-pressed={marcado}
      onClick={aoEscolher}
      className={cn(
        'h-10 shrink-0 rounded-full px-3.5 text-sm',
        escondidaNoCelular && 'hidden md:inline-block',
        marcado
          ? 'bg-primary font-semibold text-primary-foreground'
          : 'border border-border text-muted-foreground hover:text-foreground',
      )}
    >
      {children}
    </button>
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

type Mudar = <K extends FieldPath<ValoresDoFormulario>>(
  nome: K,
  valor: FieldPathValue<ValoresDoFormulario, K>,
) => void

/** Parcelas no cartão (1 = à vista) e, para compra em andamento, a parcela que cai agora. */
function CamposDeParcelas({
  valores,
  mudar,
  erros,
}: {
  valores: ValoresDoFormulario
  mudar: Mudar
  erros: FieldErrors<ValoresDoFormulario>
}) {
  const parcelado = valores.parcelas > 1
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <label htmlFor="parcelas" className={rotulo}>
          Parcelas
        </label>
        <input
          id="parcelas"
          type="number"
          inputMode="numeric"
          min={1}
          max={MAXIMO_PARCELAS}
          value={Number.isNaN(valores.parcelas) ? '' : valores.parcelas}
          onChange={(e) => mudar('parcelas', e.target.valueAsNumber)}
          aria-invalid={erros.parcelas ? true : undefined}
          aria-describedby={erros.parcelas ? 'erro-parcelas' : parcelado ? 'resumo-parcelas' : undefined}
          className={cn(campo, 'h-10 w-20')}
        />
        {parcelado && valores.centavos >= valores.parcelas && (
          <p id="resumo-parcelas" className="text-sm text-muted-foreground">
            {resumoDasParcelas(valores.centavos, valores.parcelas)} (o valor é o total da compra)
          </p>
        )}
      </div>
      <Erro id="erro-parcelas" mensagem={erros.parcelas?.message} />

      {parcelado && (
        <div className="flex flex-col gap-1.5">
          <label className="flex min-h-11 items-center gap-3 text-sm">
            <input
              type="checkbox"
              checked={valores.emAndamento}
              onChange={(e) => mudar('emAndamento', e.target.checked)}
              className="size-5 accent-[var(--color-primary)]"
            />
            Compra já em andamento (de antes do histórico)
          </label>
          {valores.emAndamento && (
            <>
              <label htmlFor="parcela-atual" className={rotulo}>
                Parcela atual
              </label>
              <input
                id="parcela-atual"
                type="number"
                inputMode="numeric"
                min={1}
                max={valores.parcelas}
                value={Number.isNaN(valores.parcelaAtual) ? '' : valores.parcelaAtual}
                onChange={(e) => mudar('parcelaAtual', e.target.valueAsNumber)}
                aria-invalid={erros.parcelaAtual ? true : undefined}
                aria-describedby={erros.parcelaAtual ? 'erro-parcela-atual' : 'dica-parcela-atual'}
                className={cn(campo, 'h-10 w-20')}
              />
              <p id="dica-parcela-atual" className="text-sm text-muted-foreground">
                Use a data da compra original. Só a parcela atual e as seguintes entram.
              </p>
              <Erro id="erro-parcela-atual" mensagem={erros.parcelaAtual?.message} />
            </>
          )}
        </div>
      )}
    </div>
  )
}
