import { http, HttpResponse } from 'msw'

/** Mesmos ids fixos da migração V3 do backend (dono local, Casa e Empresa). */
export const ESPACOS_PADRAO = [
  { id: '019a0000-0000-7000-8000-000000000011', tipo: 'HOME', nome: 'Casa', papel: 'OWNER' },
  { id: '019a0000-0000-7000-8000-000000000012', tipo: 'COMPANY', nome: 'Empresa', papel: 'OWNER' },
] as const

/** Curinga na origem: o cliente usa a origem da página (no jsdom, http://localhost:3000). */
export const URL_ESPACOS = '*/api/v1/espacos'

/** Respostas que todo teste tem de graça; um teste sobrescreve com server.use(...). */
export const handlersPadrao = [http.get(URL_ESPACOS, () => HttpResponse.json(ESPACOS_PADRAO))]
