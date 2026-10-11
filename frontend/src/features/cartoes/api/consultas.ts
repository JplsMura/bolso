import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { chaveDosCartoes } from '@/shared/api/cartoes'
import { api } from '@/shared/api/client'
import { falhar } from '@/shared/api/erro'
import type {
  AcaoDaFatura,
  CartaoApi,
  EditarCartaoApi,
  FaturaDoCartaoApi,
  FaturasDoMesApi,
  NovoCartaoApi,
} from '../model/tipos'

// cartões e metas vivem em shared (a feature lancamentos também usa); a ui desta feature os pega por aqui
export { useCartoes } from '@/shared/api/cartoes'
export { useMetas } from '@/shared/api/metas'
export { ehConflito, mensagemDoErro } from '@/shared/api/erro'

/** Um cartão por item ativo, com a fatura do mês ("2026-10"). A anterior fica na tela enquanto a nova chega. */
export function useFaturasDoMes(espacoId: string, mes: string) {
  return useQuery({
    queryKey: [...chaveDosCartoes(espacoId), 'faturas', mes] as const,
    placeholderData: (anterior, consultaAnterior) =>
      consultaAnterior?.queryKey[1] === espacoId ? anterior : undefined,
    queryFn: async (): Promise<FaturasDoMesApi> => {
      const { data, error, response } = await api.GET('/api/v1/espacos/{espacoId}/faturas', {
        params: { path: { espacoId }, query: { mes } },
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível carregar as faturas.')
      return data
    },
  })
}

export function useCriarCartao(espacoId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (corpo: NovoCartaoApi): Promise<CartaoApi> => {
      const { data, error, response } = await api.POST('/api/v1/espacos/{espacoId}/cartoes', {
        params: { path: { espacoId } },
        body: corpo,
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível salvar o cartão.')
      return data
    },
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaveDosCartoes(espacoId) }),
  })
}

export function useEditarCartao(espacoId: string, id: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (corpo: EditarCartaoApi): Promise<CartaoApi> => {
      const { data, error, response } = await api.PUT('/api/v1/espacos/{espacoId}/cartoes/{id}', {
        params: { path: { espacoId, id } },
        body: corpo,
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível salvar o cartão.')
      return data
    },
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaveDosCartoes(espacoId) }),
  })
}

export function useArquivarCartao(espacoId: string, id: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (): Promise<void> => {
      const { error, response } = await api.DELETE('/api/v1/espacos/{espacoId}/cartoes/{id}', {
        params: { path: { espacoId, id } },
      })
      if (!response.ok) falhar(response, error, 'Não foi possível arquivar o cartão.')
    },
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaveDosCartoes(espacoId) }),
  })
}

export type MudancaDaFatura = { faturaId: string; acao: AcaoDaFatura; versao: number; pagaEm?: string }

/** Fechar, pagar, reabrir ou desfazer o pagamento. Em qualquer resultado, a tela relê (409 traz a versão nova). */
export function useMudarFatura(espacoId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async ({ faturaId, acao, versao, pagaEm }: MudancaDaFatura): Promise<FaturaDoCartaoApi> => {
      const params = { path: { espacoId, id: faturaId } }
      const { data, error, response } =
        acao === 'fechar'
          ? await api.POST('/api/v1/espacos/{espacoId}/faturas/{id}/fechar', { params, body: { versao } })
          : acao === 'pagar'
            ? await api.POST('/api/v1/espacos/{espacoId}/faturas/{id}/pagar', {
                params,
                body: { versao, ...(pagaEm ? { pagaEm } : {}) },
              })
            : acao === 'reabrir'
              ? await api.POST('/api/v1/espacos/{espacoId}/faturas/{id}/reabrir', {
                  params,
                  body: { versao },
                })
              : await api.POST('/api/v1/espacos/{espacoId}/faturas/{id}/desfazer-pagamento', {
                  params,
                  body: { versao },
                })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível mudar a fatura.')
      return data
    },
    onSettled: () => cliente.invalidateQueries({ queryKey: chaveDosCartoes(espacoId) }),
  })
}
