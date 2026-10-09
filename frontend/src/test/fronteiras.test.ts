// @vitest-environment node
/// <reference types="node" />
import path from 'node:path'
import { ESLint } from 'eslint'
import { describe, expect, it } from 'vitest'

/**
 * Garante que o lint de fronteiras continua mordendo. Lint em código de mentira,
 * num arquivo que não existe, mas com o caminho de onde ele estaria.
 */
const eslint = new ESLint({ cwd: process.cwd() })

async function erros(arquivo: string, codigo: string, regra: string) {
  const [resultado] = await eslint.lintText(codigo, { filePath: path.resolve(process.cwd(), arquivo) })
  return (resultado?.messages ?? []).filter((m) => m.ruleId === regra)
}

const errosDeFronteira = (arquivo: string, codigo: string) =>
  erros(arquivo, codigo, 'boundaries/dependencies')

describe('fronteiras do front', () => {
  it('bloqueia uma feature importando outra', async () => {
    const erros = await errosDeFronteira(
      'src/features/lancamentos/ui/Lista.tsx',
      "import * as cartoes from '@/features/cartoes'\nexport const x = cartoes\n",
    )
    expect(erros).toHaveLength(1)
  })

  it('bloqueia shared importando features', async () => {
    const erros = await errosDeFronteira(
      'src/shared/lib/util.ts',
      "import * as orcamento from '@/features/orcamento'\nexport const x = orcamento\n",
    )
    expect(erros).toHaveLength(1)
  })

  it('bloqueia ui de feature chamando o cliente da API direto', async () => {
    const erros = await errosDeFronteira(
      'src/features/lancamentos/ui/Lista.tsx',
      "import { api } from '@/shared/api/client'\nexport const x = api\n",
    )
    expect(erros).toHaveLength(1)
  })

  it('permite a api da feature usar o cliente e a ui usar shared', async () => {
    const daApi = await errosDeFronteira(
      'src/features/lancamentos/api/consultas.ts',
      "import { api } from '@/shared/api/client'\nexport const x = api\n",
    )
    const daUi = await errosDeFronteira(
      'src/features/lancamentos/ui/Lista.tsx',
      "import { cn } from '@/shared/lib/cn'\nimport { formatarBRL } from '@/shared/lib/money'\nexport const x = [cn, formatarBRL]\n",
    )
    expect(daApi).toEqual([])
    expect(daUi).toEqual([])
  })

  it('permite model e api usarem os tipos do contrato, mas só a api usa o cliente', async () => {
    const tiposNoModel = await errosDeFronteira(
      'src/features/lancamentos/model/tipos.ts',
      "import type { components } from '@/shared/api/schema'\nexport type X = components['schemas']\n",
    )
    const clienteNoModel = await errosDeFronteira(
      'src/features/lancamentos/model/regras.ts',
      "import { api } from '@/shared/api/client'\nexport const x = api\n",
    )
    expect(tiposNoModel).toEqual([])
    expect(clienteNoModel).toHaveLength(1)
  })

  it('permite rotas usarem a fachada da feature', async () => {
    const erros = await errosDeFronteira(
      'src/routes/$espaco/lancamentos.tsx',
      "import * as lancamentos from '@/features/lancamentos'\nexport const x = lancamentos\n",
    )
    expect(erros).toEqual([])
  })
  it('permite teste de feature usar os utilitários de src/test, mas não código de produção', async () => {
    const doTeste = await errosDeFronteira(
      'src/features/lancamentos/ui/Lista.test.tsx',
      "import { renderizarRota } from '@/test/renderizarRota'\nexport const x = renderizarRota\n",
    )
    const daProducao = await errosDeFronteira(
      'src/features/lancamentos/ui/Lista.tsx',
      "import { renderizarRota } from '@/test/renderizarRota'\nexport const x = renderizarRota\n",
    )
    expect(doTeste).toEqual([])
    expect(daProducao).toHaveLength(1)
  })
})

describe('dinheiro', () => {
  it('bloqueia parseFloat e Number.parseFloat', async () => {
    const global = await erros(
      'src/features/lancamentos/ui/Valor.tsx',
      "export const x = parseFloat('1.1')\n",
      'no-restricted-syntax',
    )
    const metodo = await erros(
      'src/features/lancamentos/ui/Valor.tsx',
      "export const x = Number.parseFloat('1.1')\n",
      'no-restricted-syntax',
    )
    expect(global).toHaveLength(1)
    expect(metodo).toHaveLength(1)
  })
})
