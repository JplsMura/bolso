import { setupServer } from 'msw/node'

/** API simulada nos testes. Cada teste registra os handlers de que precisa com server.use(...). */
export const server = setupServer()
