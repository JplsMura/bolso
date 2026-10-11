import { useNavigate } from '@tanstack/react-router'
import { type SlugDoEspaco, useEspacoAtual } from '@/shared/lib/espacoAtual'
import { mensagemDoErro, useCriarCartao } from '../api/consultas'
import { CARTAO_VAZIO, paraRequisicao, type ValoresDoCartao } from '../model/formulario'
import { CabecalhoDoCartao } from '../ui/CabecalhoDoCartao'
import { FormularioDeCartao } from '../ui/FormularioDeCartao'

export function NovoCartaoPage({ espaco }: { espaco: SlugDoEspaco }) {
  const { id: espacoId } = useEspacoAtual()
  const navegar = useNavigate()
  const criar = useCriarCartao(espacoId)

  async function enviar(valores: ValoresDoCartao) {
    try {
      await criar.mutateAsync(paraRequisicao(valores))
      await navegar({ to: '/$espaco/cartoes', params: { espaco }, search: {} })
    } catch {
      // o motivo aparece no formulário, a partir de criar.error
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-col gap-5">
      <CabecalhoDoCartao titulo="Novo cartão" espaco={espaco} />
      <FormularioDeCartao
        valoresIniciais={CARTAO_VAZIO}
        rotuloEnviar="Salvar cartão"
        enviando={criar.isPending}
        aoEnviar={enviar}
        erro={
          criar.isError
            ? mensagemDoErro(criar.error, 'Não foi possível salvar o cartão. Tente de novo.')
            : undefined
        }
      />
    </div>
  )
}
