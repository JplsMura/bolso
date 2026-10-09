import { setupServer } from 'msw/node'
import { handlersPadrao } from './handlers'

/**
 * API simulada nos testes. Já responde os espaços; cada teste registra o que mais precisar com
 * server.use(...), e o setup volta ao padrão depois de cada teste.
 */
export const server = setupServer(...handlersPadrao)
