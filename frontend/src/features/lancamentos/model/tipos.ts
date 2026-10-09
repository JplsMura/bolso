import type { components } from '@/shared/api/schema'

export type LancamentoApi = components['schemas']['LancamentoResponse']
export type ListaDoMesApi = components['schemas']['ListaDoMesResponse']
export type NovoLancamentoApi = components['schemas']['NovoLancamentoRequest']
export type EditarLancamentoApi = components['schemas']['EditarLancamentoRequest']
export type TagApi = components['schemas']['TagResponse']
export type MetaApi = components['schemas']['MetaResponse']

export type Direcao = LancamentoApi['direcao']
export type FormaPagamento = NonNullable<LancamentoApi['formaPagamento']>
