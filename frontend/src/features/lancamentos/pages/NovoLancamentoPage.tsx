import { useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { type SlugDoEspaco, useEspacoAtual } from '@/shared/lib/espacoAtual'
import { useCriarLancamento } from '../api/consultas'
import { mensagemDoErro } from '../api/erro'
import { diaDe } from '../model/datas'
import { paraRequisicao, type ValoresDoFormulario, valoresIniciais } from '../model/formulario'
import { CabecalhoDeFormulario } from '../ui/CabecalhoDeFormulario'
import { FormularioDeLancamento } from '../ui/FormularioDeLancamento'

type Props = { espaco: SlugDoEspaco; tipo?: 'entrada' | undefined }

export function NovoLancamentoPage({ espaco, tipo }: Props) {
  const { id: espacoId, tipo: tipoEspaco } = useEspacoAtual()
  const navegar = useNavigate()
  const criar = useCriarLancamento(espacoId)
  const [iniciais] = useState(() => valoresIniciais(tipo === 'entrada' ? 'IN' : 'OUT', diaDe(new Date())))

  async function enviar(valores: ValoresDoFormulario) {
    try {
      const salvo = await criar.mutateAsync(paraRequisicao(valores, tipoEspaco === 'HOME'))
      await navegar({ to: '/$espaco/lancamentos', params: { espaco }, search: { mes: salvo.mesReferencia } })
    } catch {
      // o motivo aparece no formulário, a partir de criar.error
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-col gap-5">
      <CabecalhoDeFormulario titulo="Novo lançamento" espaco={espaco} />
      <FormularioDeLancamento
        espacoId={espacoId}
        tipoEspaco={tipoEspaco}
        valoresIniciais={iniciais}
        rotuloEnviar="Salvar"
        enviando={criar.isPending}
        aoEnviar={enviar}
        erro={
          criar.isError
            ? mensagemDoErro(criar.error, 'Não foi possível salvar o lançamento. Tente de novo.')
            : undefined
        }
      />
    </div>
  )
}
