import createClient from 'openapi-fetch'
import type { paths } from './schema'

/** Cliente tipado pelo contrato OpenAPI. Sessão por cookie HttpOnly, por isso credentials: include. */
export const api = createClient<paths>({ baseUrl: '/', credentials: 'include' })
