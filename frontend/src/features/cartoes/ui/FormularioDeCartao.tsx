import { zodResolver } from '@hookform/resolvers/zod'
import type { ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { centavosDeDigitos, formatarBRL } from '@/shared/lib/money'
import { schemaDoCartao, type ValoresDoCartao } from '../model/formulario'

type Props = {
  valoresIniciais: ValoresDoCartao
  rotuloEnviar: string
  enviando: boolean
  aoEnviar: (valores: ValoresDoCartao) => Promise<void>
  erro?: string | undefined
  acoesExtras?: ReactNode
}

const rotulo = 'text-[13px] text-muted-foreground'
const campo = 'h-11 w-full rounded-xl border border-border bg-card px-3.5 text-[15px]'

/** Nome, dias de fechamento e vencimento, e limite. */
export function FormularioDeCartao({
  valoresIniciais,
  rotuloEnviar,
  enviando,
  aoEnviar,
  erro,
  acoesExtras,
}: Props) {
  const { handleSubmit, register, setValue, watch, formState } = useForm<ValoresDoCartao>({
    resolver: zodResolver(schemaDoCartao),
    defaultValues: valoresIniciais,
  })
  const { errors, isSubmitted } = formState
  // O React Compiler não está ligado neste projeto, então o aviso de memoização do watch() não se aplica.
  // eslint-disable-next-line react-hooks/incompatible-library
  const limite = watch('limiteCentavos')

  return (
    <form onSubmit={(e) => void handleSubmit(aoEnviar)(e)} noValidate className="flex flex-col gap-5">
      <Campo id="nome" rotulo="Nome" erro={errors.nome?.message}>
        <input
          id="nome"
          autoComplete="off"
          placeholder="Por exemplo: Nubank"
          aria-invalid={errors.nome ? true : undefined}
          aria-describedby={errors.nome ? 'erro-nome' : undefined}
          className={campo}
          {...register('nome')}
        />
      </Campo>

      <div className="grid grid-cols-2 gap-3">
        <Campo id="dia-fechamento" rotulo="Dia de fechamento" erro={errors.diaFechamento?.message}>
          <input
            id="dia-fechamento"
            type="number"
            inputMode="numeric"
            min={1}
            max={31}
            aria-invalid={errors.diaFechamento ? true : undefined}
            aria-describedby={errors.diaFechamento ? 'erro-dia-fechamento' : undefined}
            className={campo}
            {...register('diaFechamento', { valueAsNumber: true })}
          />
        </Campo>
        <Campo id="dia-vencimento" rotulo="Dia de vencimento" erro={errors.diaVencimento?.message}>
          <input
            id="dia-vencimento"
            type="number"
            inputMode="numeric"
            min={1}
            max={31}
            aria-invalid={errors.diaVencimento ? true : undefined}
            aria-describedby={errors.diaVencimento ? 'erro-dia-vencimento' : undefined}
            className={campo}
            {...register('diaVencimento', { valueAsNumber: true })}
          />
        </Campo>
      </div>
      <p className="-mt-2 text-sm text-muted-foreground">
        Compra no dia do fechamento ou depois entra na fatura do mês seguinte.
      </p>

      <Campo id="limite" rotulo="Limite" erro={errors.limiteCentavos?.message}>
        <input
          id="limite"
          inputMode="numeric"
          autoComplete="off"
          value={formatarBRL(limite)}
          onChange={(e) =>
            setValue('limiteCentavos', centavosDeDigitos(e.target.value), {
              shouldDirty: true,
              shouldValidate: isSubmitted,
            })
          }
          aria-invalid={errors.limiteCentavos ? true : undefined}
          aria-describedby={errors.limiteCentavos ? 'erro-limite' : undefined}
          className={`${campo} tabular-nums`}
        />
      </Campo>

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

function Campo({
  id,
  rotulo: texto,
  erro,
  children,
}: {
  id: string
  rotulo: string
  erro?: string | undefined
  children: ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className={rotulo}>
        {texto}
      </label>
      {children}
      {erro && (
        <p id={`erro-${id}`} role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
    </div>
  )
}
