import { useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { type SlugDoEspaco, useEspacoAtual } from '@/shared/lib/espacoAtual'
import { useCriarLancamento, useCriarParcelamento } from '../api/consultas'
import { ehFaturaFechada, mensagemDoErro } from '../api/erro'
import { diaDe } from '../model/datas'
import {
  ehParcelado,
  paraParcelamento,
  paraRequisicao,
  type ValoresDoFormulario,
  valoresIniciais,
} from '../model/formulario'
import { CabecalhoDeFormulario } from '../ui/CabecalhoDeFormulario'
import { FormularioDeLancamento } from '../ui/FormularioDeLancamento'

type Props = { espaco: SlugDoEspaco; tipo?: 'entrada' | undefined }

export function NovoLancamentoPage({ espaco, tipo }: Props) {
  const { id: espacoId, tipo: tipoEspaco } = useEspacoAtual()
  const navegar = useNavigate()
  const criar = useCriarLancamento(espacoId)
  const parcelar = useCriarParcelamento(espacoId)
  const [iniciais] = useState(() => valoresIniciais(tipo === 'entrada' ? 'IN' : 'OUT', diaDe(new Date())))

  const comMeta = tipoEspaco === 'HOME'
  // o erro que aparece é o da última tentativa (à vista ou parcelada)
  const [ultima, setUltima] = useState<'avista' | 'parcelado'>('avista')
  const falha = ultima === 'parcelado' ? parcelar : criar

  async function enviar(valores: ValoresDoFormulario) {
    try {
      if (ehParcelado(valores)) {
        setUltima('parcelado')
        criar.reset()
        const salvo = await parcelar.mutateAsync(paraParcelamento(valores, comMeta))
        const primeira = salvo.itens[0]?.data ?? valores.data
        await navegar({
          to: '/$espaco/lancamentos',
          params: { espaco },
          search: { mes: primeira.slice(0, 7) },
        })
        return
      }
      setUltima('avista')
      parcelar.reset()
      const salvo = await criar.mutateAsync(paraRequisicao(valores, comMeta))
      await navegar({ to: '/$espaco/lancamentos', params: { espaco }, search: { mes: salvo.mesReferencia } })
    } catch {
      // o motivo aparece no formulário, a partir do erro da mutação
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-lg md:max-w-5xl flex-col gap-5">
      <CabecalhoDeFormulario titulo="Novo lançamento" espaco={espaco} />
      <FormularioDeLancamento
        espacoId={espacoId}
        tipoEspaco={tipoEspaco}
        valoresIniciais={iniciais}
        rotuloEnviar="Salvar"
        enviando={criar.isPending || parcelar.isPending}
        aoEnviar={enviar}
        permiteParcelar
        erro={
          falha.isError
            ? mensagemDoErro(falha.error, 'Não foi possível salvar o lançamento. Tente de novo.')
            : undefined
        }
        erroDeFaturaFechada={falha.isError && ehFaturaFechada(falha.error)}
      />
    </div>
  )
}
