import { type QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { chaveDosCartoes } from '@/shared/api/cartoes'
import { api } from '@/shared/api/client'
import type { Filtros } from '../model/filtros'
import { falhar } from './erro'
import type {
  EditarLancamentoApi,
  EditarParcelamentoApi,
  FaturaDaCompraApi,
  LancamentoApi,
  ListaDoMesApi,
  NovoLancamentoApi,
  NovoParcelamentoApi,
  ParcelamentoApi,
  TagApi,
} from '../model/tipos'

// metas e cartões vivem em shared (a feature cartoes também usa); a ui desta feature os pega por aqui
export { useCartoes } from '@/shared/api/cartoes'
export { useMetas } from '@/shared/api/metas'

const RAIZ = 'lancamentos'

const chaves = {
  tudo: (espacoId: string) => [RAIZ, espacoId] as const,
  lista: (espacoId: string, mes: string, filtros: Filtros) =>
    [RAIZ, espacoId, 'lista', mes, filtros] as const,
  um: (espacoId: string, id: string) => [RAIZ, espacoId, 'um', id] as const,
  tags: (espacoId: string) => [RAIZ, espacoId, 'tags'] as const,
  parcelamento: (espacoId: string, id: string) => [RAIZ, espacoId, 'parcelamento', id] as const,
}

/** Gravar um gasto mexe na lista e, se for no cartão, no limite e nas faturas. */
function invalidarTudo(cliente: QueryClient, espacoId: string) {
  return Promise.all([
    cliente.invalidateQueries({ queryKey: chaves.tudo(espacoId) }),
    cliente.invalidateQueries({ queryKey: chaveDosCartoes(espacoId) }),
  ])
}

const MSG_CARREGAR = 'Não foi possível carregar os lançamentos.'

/** Lançamentos do mês ("2026-03") e os totais; a lista anterior fica na tela enquanto a nova chega. */
export function useLancamentosDoMes(espacoId: string, mes: string, filtros: Filtros) {
  return useQuery({
    queryKey: chaves.lista(espacoId, mes, filtros),
    // a lista anterior só serve de apoio ao trocar de mês ou filtro, nunca ao trocar de espaço
    placeholderData: (anterior, consultaAnterior) =>
      consultaAnterior?.queryKey[1] === espacoId ? anterior : undefined,
    queryFn: async (): Promise<ListaDoMesApi> => {
      const query = {
        mes,
        ...(filtros.texto.trim() ? { q: filtros.texto.trim() } : {}),
        ...(filtros.metaId ? { metaId: filtros.metaId } : {}),
        ...(filtros.tagId ? { tagId: filtros.tagId } : {}),
        ...(filtros.pagamento ? { pagamento: filtros.pagamento } : {}),
        ...(filtros.direcao ? { direcao: filtros.direcao } : {}),
        ...(filtros.cartaoId ? { cartaoId: filtros.cartaoId } : {}),
      }
      const { data, error, response } = await api.GET('/api/v1/espacos/{espacoId}/lancamentos', {
        params: { path: { espacoId }, query },
      })
      if (!response.ok || !data) falhar(response, error, MSG_CARREGAR)
      return data
    },
  })
}

export function useLancamento(espacoId: string, id: string) {
  return useQuery({
    queryKey: chaves.um(espacoId, id),
    retry: false,
    queryFn: async (): Promise<LancamentoApi> => {
      const { data, error, response } = await api.GET('/api/v1/espacos/{espacoId}/lancamentos/{id}', {
        params: { path: { espacoId, id } },
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível carregar o lançamento.')
      return data
    },
  })
}

export function useTags(espacoId: string) {
  return useQuery({
    queryKey: chaves.tags(espacoId),
    queryFn: async (): Promise<TagApi[]> => {
      const { data, error, response } = await api.GET('/api/v1/espacos/{espacoId}/tags', {
        params: { path: { espacoId } },
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível carregar as tags.')
      return data
    },
  })
}

export function useCriarTag(espacoId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (nome: string): Promise<TagApi> => {
      const { data, error, response } = await api.POST('/api/v1/espacos/{espacoId}/tags', {
        params: { path: { espacoId } },
        body: { nome },
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível criar a tag.')
      return data
    },
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.tags(espacoId) }),
  })
}

export function useCriarLancamento(espacoId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (corpo: NovoLancamentoApi): Promise<LancamentoApi> => {
      const { data, error, response } = await api.POST('/api/v1/espacos/{espacoId}/lancamentos', {
        params: { path: { espacoId } },
        body: corpo,
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível salvar o lançamento.')
      return data
    },
    onSuccess: () => invalidarTudo(cliente, espacoId),
  })
}

export function useEditarLancamento(espacoId: string, id: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (corpo: EditarLancamentoApi): Promise<LancamentoApi> => {
      const { data, error, response } = await api.PUT('/api/v1/espacos/{espacoId}/lancamentos/{id}', {
        params: { path: { espacoId, id } },
        body: corpo,
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível salvar o lançamento.')
      return data
    },
    onSuccess: () => invalidarTudo(cliente, espacoId),
  })
}

export function useExcluirLancamento(espacoId: string, id: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (): Promise<void> => {
      const { error, response } = await api.DELETE('/api/v1/espacos/{espacoId}/lancamentos/{id}', {
        params: { path: { espacoId, id } },
      })
      if (!response.ok) falhar(response, error, 'Não foi possível excluir o lançamento.')
    },
    onSuccess: () => invalidarTudo(cliente, espacoId),
  })
}

/** Em qual fatura cai uma compra no cartão nessa data. Só consulta com cartão e data escolhidos. */
export function useFaturaDaCompra(espacoId: string, cartaoId: string | null, data: string) {
  const valida = /^\d{4}-\d{2}-\d{2}$/.test(data)
  return useQuery({
    queryKey: [...chaveDosCartoes(espacoId), 'fatura-da-compra', cartaoId, data] as const,
    enabled: cartaoId !== null && valida,
    queryFn: async (): Promise<FaturaDaCompraApi> => {
      const {
        data: fatura,
        error,
        response,
      } = await api.GET('/api/v1/espacos/{espacoId}/cartoes/{id}/fatura-da-compra', {
        params: { path: { espacoId, id: cartaoId ?? '' }, query: { data } },
      })
      if (!response.ok || !fatura) falhar(response, error, 'Não foi possível saber a fatura desta compra.')
      return fatura
    },
  })
}

export function useCriarParcelamento(espacoId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (corpo: NovoParcelamentoApi): Promise<ParcelamentoApi> => {
      const { data, error, response } = await api.POST('/api/v1/espacos/{espacoId}/parcelamentos', {
        params: { path: { espacoId } },
        body: corpo,
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível salvar o parcelamento.')
      return data
    },
    onSuccess: () => invalidarTudo(cliente, espacoId),
  })
}

export function useParcelamento(espacoId: string, id: string) {
  return useQuery({
    queryKey: chaves.parcelamento(espacoId, id),
    retry: false,
    queryFn: async (): Promise<ParcelamentoApi> => {
      const { data, error, response } = await api.GET('/api/v1/espacos/{espacoId}/parcelamentos/{id}', {
        params: { path: { espacoId, id } },
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível carregar o parcelamento.')
      return data
    },
  })
}

export function useEditarParcelamento(espacoId: string, id: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (corpo: EditarParcelamentoApi): Promise<ParcelamentoApi> => {
      const { data, error, response } = await api.PUT('/api/v1/espacos/{espacoId}/parcelamentos/{id}', {
        params: { path: { espacoId, id } },
        body: corpo,
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível salvar o parcelamento.')
      return data
    },
    onSuccess: () => invalidarTudo(cliente, espacoId),
  })
}

export function useExcluirParcelamento(espacoId: string, id: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (): Promise<void> => {
      const { error, response } = await api.DELETE('/api/v1/espacos/{espacoId}/parcelamentos/{id}', {
        params: { path: { espacoId, id } },
      })
      if (!response.ok) falhar(response, error, 'Não foi possível excluir as parcelas.')
    },
    onSuccess: () => invalidarTudo(cliente, espacoId),
  })
}
