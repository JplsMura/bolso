import type { components } from '@/shared/api/schema'

export type LancamentoApi = components['schemas']['LancamentoResponse']
export type ListaDoMesApi = components['schemas']['ListaDoMesResponse']
export type NovoLancamentoApi = components['schemas']['NovoLancamentoRequest']
export type EditarLancamentoApi = components['schemas']['EditarLancamentoRequest']
export type TagApi = components['schemas']['TagResponse']
export type MetaApi = components['schemas']['MetaResponse']
export type CartaoApi = components['schemas']['CartaoResponse']
export type FaturaDaCompraApi = components['schemas']['FaturaDaCompraResponse']
export type ParcelamentoApi = components['schemas']['ParcelamentoResponse']
export type NovoParcelamentoApi = components['schemas']['NovoParcelamentoRequest']
export type EditarParcelamentoApi = components['schemas']['EditarParcelamentoRequest']

export type Direcao = LancamentoApi['direcao']
export type FormaPagamento = NonNullable<LancamentoApi['formaPagamento']>
