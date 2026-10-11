import { Link, useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { type SlugDoEspaco, useEspacoAtual } from '@/shared/lib/espacoAtual'
import { EstadoDeErro } from '@/shared/ui/EstadoDeErro'
import { ehConflito, mensagemDoErro, useArquivarCartao, useCartoes, useEditarCartao } from '../api/consultas'
import { deCartao, paraRequisicao, type ValoresDoCartao } from '../model/formulario'
import type { CartaoApi } from '../model/tipos'
import { CabecalhoDoCartao } from '../ui/CabecalhoDoCartao'
import { Confirmacao } from '../ui/Confirmacao'
import { FormularioDeCartao } from '../ui/FormularioDeCartao'

type Props = { espaco: SlugDoEspaco; id: string }

export function EditarCartaoPage({ espaco, id }: Props) {
  const { id: espacoId } = useEspacoAtual()
  const cartoes = useCartoes(espacoId)

  if (cartoes.isPending) {
    return (
      <p role="status" className="text-[15px] text-muted-foreground">
        Carregando cartão…
      </p>
    )
  }
  if (!cartoes.data) {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-col gap-5">
        <CabecalhoDoCartao titulo="Cartão" espaco={espaco} />
        <EstadoDeErro
          mensagem="Não foi possível carregar o cartão."
          aoTentarDeNovo={() => void cartoes.refetch()}
          tentando={cartoes.isFetching}
        />
      </div>
    )
  }
  const cartao = cartoes.data.find((c) => c.id === id)
  if (!cartao) {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-col gap-5">
        <CabecalhoDoCartao titulo="Cartão" espaco={espaco} />
        <div className="flex flex-col items-start gap-3 rounded-2xl border border-border bg-card p-5">
          <p role="alert" className="text-[15px]">
            Cartão não encontrado. Ele pode ter sido arquivado.
          </p>
          <Link to="/$espaco/cartoes" params={{ espaco }} search={{}} className="text-primary underline">
            Voltar para Cartões
          </Link>
        </div>
      </div>
    )
  }
  return <Edicao key={`${cartao.id}-${cartao.versao}`} espaco={espaco} espacoId={espacoId} cartao={cartao} />
}

function Edicao({ espaco, espacoId, cartao }: { espaco: SlugDoEspaco; espacoId: string; cartao: CartaoApi }) {
  const navegar = useNavigate()
  const editar = useEditarCartao(espacoId, cartao.id)
  const arquivar = useArquivarCartao(espacoId, cartao.id)
  const [confirmando, setConfirmando] = useState(false)
  const voltar = () => navegar({ to: '/$espaco/cartoes', params: { espaco }, search: {} })

  async function enviar(valores: ValoresDoCartao) {
    try {
      await editar.mutateAsync({ ...paraRequisicao(valores), versao: cartao.versao })
      await voltar()
    } catch {
      // o motivo aparece no formulário
    }
  }

  async function confirmarArquivo() {
    try {
      await arquivar.mutateAsync()
      await voltar()
    } catch {
      // o motivo aparece na confirmação
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-col gap-5">
      <CabecalhoDoCartao titulo="Editar cartão" espaco={espaco} />
      <FormularioDeCartao
        valoresIniciais={deCartao(cartao)}
        rotuloEnviar="Salvar alterações"
        enviando={editar.isPending}
        aoEnviar={enviar}
        erro={
          editar.isError
            ? ehConflito(editar.error)
              ? 'Este cartão foi alterado em outro lugar. Recarregue a página e refaça a edição.'
              : mensagemDoErro(editar.error, 'Não foi possível salvar o cartão. Tente de novo.')
            : undefined
        }
        acoesExtras={
          <button
            type="button"
            onClick={() => setConfirmando(true)}
            className="h-12 rounded-xl border border-destructive/60 px-5 text-[15px] text-destructive"
          >
            Arquivar
          </button>
        }
      />
      {confirmando && (
        <Confirmacao
          titulo="Arquivar este cartão?"
          texto={`O ${cartao.nome} sai da lista e do formulário de gasto. Os gastos e as faturas dele continuam. Só dá para arquivar com todas as faturas pagas.`}
          rotuloConfirmar="Arquivar"
          perigo
          ocupado={arquivar.isPending}
          erro={
            arquivar.isError
              ? mensagemDoErro(arquivar.error, 'Não foi possível arquivar o cartão.')
              : undefined
          }
          aoCancelar={() => {
            arquivar.reset()
            setConfirmando(false)
          }}
          aoConfirmar={() => void confirmarArquivo()}
        />
      )}
    </div>
  )
}
