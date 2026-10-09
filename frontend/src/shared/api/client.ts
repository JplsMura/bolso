import createClient from 'openapi-fetch'
import type { paths } from './schema'

/**
 * Cliente tipado pelo contrato OpenAPI. Sessão por cookie HttpOnly, por isso credentials: include.
 * A URL base é a origem da página (o Vite e o nginx repassam /api para a API); o fetch é lido a cada
 * chamada para o MSW dos testes conseguir interceptar.
 */
export const api = createClient<paths>({
  baseUrl: globalThis.location?.origin ?? '/',
  credentials: 'include',
  fetch: (requisicao) => globalThis.fetch(requisicao),
})
