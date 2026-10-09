import { Link, useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { type SlugDoEspaco, useEspacoAtual } from '@/shared/lib/espacoAtual'
import { useEditarLancamento, useExcluirLancamento, useLancamento } from '../api/consultas'
import { ehConflito, ehNaoEncontrado, mensagemDoErro } from '../api/erro'
import { deLancamento, paraRequisicao, type ValoresDoFormulario } from '../model/formulario'
import { CabecalhoDeFormulario } from '../ui/CabecalhoDeFormulario'
import { ConfirmarExclusao } from '../ui/ConfirmarExclusao'
import { EstadoDeErro } from '../ui/EstadoDeErro'
import { FormularioDeLancamento } from '../ui/FormularioDeLancamento'

type Props = { espaco: SlugDoEspaco; id: string }

export function EditarLancamentoPage({ espaco, id }: Props) {
  const { id: espacoId, tipo: tipoEspaco } = useEspacoAtual()
  const navegar = useNavigate()
  const lancamento = useLancamento(espacoId, id)
  const editar = useEditarLancamento(espacoId, id)
  const excluir = useExcluirLancamento(espacoId, id)
  const [confirmando, setConfirmando] = useState(false)

  if (lancamento.isPending) {
    return (
      <p role="status" className="text-[15px] text-muted-foreground">
        Carregando lançamento…
      </p>
    )
  }

  if (!lancamento.data) {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-col gap-5">
        <CabecalhoDeFormulario titulo="Lançamento" espaco={espaco} />
        {ehNaoEncontrado(lancamento.error) ? (
          <div className="flex flex-col items-start gap-3 rounded-2xl border border-border bg-card p-5">
            <p role="alert" className="text-[15px]">
              Lançamento não encontrado. Ele pode ter sido excluído.
            </p>
            <Link
              to="/$espaco/lancamentos"
              params={{ espaco }}
              search={{}}
              className="text-primary underline"
            >
              Voltar para a lista
            </Link>
          </div>
        ) : (
          <EstadoDeErro
            mensagem="Não foi possível carregar o lançamento."
            aoTentarDeNovo={() => void lancamento.refetch()}
            tentando={lancamento.isFetching}
          />
        )}
      </div>
    )
  }

  const atual = lancamento.data
  const voltarParaLista = (mes: string) =>
    navegar({ to: '/$espaco/lancamentos', params: { espaco }, search: { mes } })

  async function enviar(valores: ValoresDoFormulario) {
    try {
      const salvo = await editar.mutateAsync({
        ...paraRequisicao(valores, tipoEspaco === 'HOME'),
        versao: atual.versao,
      })
      await voltarParaLista(salvo.mesReferencia)
    } catch {
      // o motivo aparece na tela, a partir de editar.error
    }
  }

  async function confirmarExclusao() {
    try {
      await excluir.mutateAsync()
      await voltarParaLista(atual.mesReferencia)
    } catch {
      // o motivo aparece na confirmação, a partir de excluir.error
    }
  }

  async function recarregar() {
    editar.reset()
    await lancamento.refetch()
  }

  const conflito = editar.isError && ehConflito(editar.error)

  return (
    <div className="mx-auto flex w-full max-w-lg flex-col gap-5">
      <CabecalhoDeFormulario titulo="Editar lançamento" espaco={espaco} mes={atual.mesReferencia} />

      {conflito && (
        <div
          role="alert"
          className="flex flex-col items-start gap-3 rounded-2xl border border-warning/60 bg-card p-4"
        >
          <p className="text-[15px]">
            Alguém alterou este lançamento enquanto você editava. Recarregue para ver a versão atual e refazer
            a edição.
          </p>
          <button
            type="button"
            onClick={() => void recarregar()}
            disabled={lancamento.isFetching}
            className="h-11 rounded-xl border border-border px-5 text-[15px]"
          >
            Recarregar
          </button>
        </div>
      )}

      <FormularioDeLancamento
        key={`${atual.id}-${atual.versao}`}
        espacoId={espacoId}
        tipoEspaco={tipoEspaco}
        valoresIniciais={deLancamento(atual)}
        rotuloEnviar="Salvar alterações"
        enviando={editar.isPending}
        aoEnviar={enviar}
        erro={
          editar.isError && !conflito
            ? mensagemDoErro(editar.error, 'Não foi possível salvar o lançamento. Tente de novo.')
            : undefined
        }
        acoesExtras={
          <button
            type="button"
            onClick={() => setConfirmando(true)}
            className="h-12 rounded-xl border border-destructive/60 px-5 text-[15px] text-destructive"
          >
            Excluir
          </button>
        }
      />

      {confirmando && (
        <ConfirmarExclusao
          descricao={atual.descricao}
          excluindo={excluir.isPending}
          erro={
            excluir.isError
              ? mensagemDoErro(excluir.error, 'Não foi possível excluir o lançamento.')
              : undefined
          }
          aoCancelar={() => {
            excluir.reset()
            setConfirmando(false)
          }}
          aoConfirmar={() => void confirmarExclusao()}
        />
      )}
    </div>
  )
}
