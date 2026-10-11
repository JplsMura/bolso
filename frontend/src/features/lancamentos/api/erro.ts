// O erro da API vive em shared (a feature cartoes também usa). Mantido aqui para os imports da feature.
export {
  ehConflito,
  ehFaturaFechada,
  ehNaoEncontrado,
  ErroDaApi,
  falhar,
  mensagemDoErro,
} from '@/shared/api/erro'
