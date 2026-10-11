import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import type { Filtros } from '../model/filtros'
import { falhar } from './erro'
import type {
  EditarLancamentoApi,
  LancamentoApi,
  ListaDoMesApi,
  MetaApi,
  NovoLancamentoApi,
  TagApi,
} from '../model/tipos'

const RAIZ = 'lancamentos'

const chaves = {
  tudo: (espacoId: string) => [RAIZ, espacoId] as const,
  lista: (espacoId: string, mes: string, filtros: Filtros) =>
    [RAIZ, espacoId, 'lista', mes, filtros] as const,
  um: (espacoId: string, id: string) => [RAIZ, espacoId, 'um', id] as const,
  tags: (espacoId: string) => [RAIZ, espacoId, 'tags'] as const,
  metas: (espacoId: string) => [RAIZ, espacoId, 'metas'] as const,
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

/** `ativo` falso não consulta (a Empresa não tem metas). */
export function useMetas(espacoId: string, ativo = true) {
  return useQuery({
    queryKey: chaves.metas(espacoId),
    enabled: ativo,
    queryFn: async (): Promise<MetaApi[]> => {
      const { data, error, response } = await api.GET('/api/v1/espacos/{espacoId}/metas', {
        params: { path: { espacoId } },
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível carregar as metas.')
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
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.tudo(espacoId) }),
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
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.tudo(espacoId) }),
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
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.tudo(espacoId) }),
  })
}
