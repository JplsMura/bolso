// Fachada pública da feature identidade: só o que for exportado aqui pode ser usado por app/ e routes/.
// Pastas internas (criadas pela feature): api/ (hooks TanStack Query), model/ (tipos, Zod, cálculos), ui/, pages/.
export { type EspacoApi, useEspacos } from './api/useEspacos'
