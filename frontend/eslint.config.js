import js from '@eslint/js'
import boundaries from 'eslint-plugin-boundaries'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import globals from 'globals'
import tseslint from 'typescript-eslint'

/**
 * Fronteiras do front (docs/04): uma feature não importa outra; shared não importa features;
 * ui/pages de uma feature não chamam a API direto (passam pelos hooks de api/). Os tipos gerados do
 * contrato (schema.d.ts) valem para model/ e api/; o cliente (client.ts) só para api/.
 * Uma política por feature, sem templates, para a regra ficar legível.
 */
const FEATURES = ['identidade', 'lancamentos', 'cartoes', 'orcamento', 'faturamento']

/** O arquivo de tipos gerado do contrato (schema.d.ts), reconhecido por categoria de arquivo. */
const TIPOS_DA_API = { file: { categories: 'tipos-da-api' } }

const el = (type, feature) => ({
  element: feature ? { type, captured: { feature } } : { type },
})

const politicasDasFeatures = FEATURES.flatMap((f) => [
  {
    from: el('feature', f),
    allow: { to: [el('feature', f), el('feature-ui', f), el('feature-api', f), el('feature-model', f)] },
  },
  {
    from: el('feature-ui', f),
    allow: { to: [el('feature-ui', f), el('feature-api', f), el('feature-model', f), el('shared')] },
  },
  {
    from: el('feature-api', f),
    allow: {
      to: [el('feature-api', f), el('feature-model', f), el('shared'), el('shared-api'), TIPOS_DA_API],
    },
  },
  {
    from: el('feature-model', f),
    allow: { to: [el('feature-model', f), el('shared'), TIPOS_DA_API] },
  },
])

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'src/routeTree.gen.ts', 'src/shared/api/schema.d.ts'] },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, ...tseslint.configs.strict],
    languageOptions: { ecmaVersion: 2023, globals: globals.browser },
    plugins: { 'react-hooks': reactHooks, 'react-refresh': reactRefresh },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true, allowExportNames: ['Route'] },
      ],
      // dinheiro é centavos inteiros (src/shared/lib/money.ts)
      'no-restricted-syntax': [
        'error',
        {
          selector:
            "CallExpression[callee.name='parseFloat'], MemberExpression[object.name='Number'][property.name='parseFloat']",
          message: 'Dinheiro é centavos inteiros: use paraCentavos() de shared/lib/money.',
        },
      ],
    },
  },
  {
    // rotas do TanStack exportam `Route` e o componente fica no mesmo arquivo (o plugin separa no build)
    files: ['src/routes/**/*.tsx'],
    rules: { 'react-refresh/only-export-components': 'off' },
  },
  {
    files: ['src/**/*.{ts,tsx}'],
    plugins: { boundaries },
    settings: {
      'import/resolver': { typescript: { project: './tsconfig.json' } },
      'boundaries/include': ['src/**/*'],
      // a ordem importa: o primeiro padrão que casar define o tipo
      'boundaries/elements': [
        { type: 'feature-ui', pattern: 'src/features/*/ui', capture: ['feature'] },
        { type: 'feature-ui', pattern: 'src/features/*/pages', capture: ['feature'] },
        { type: 'feature-api', pattern: 'src/features/*/api', capture: ['feature'] },
        { type: 'feature-model', pattern: 'src/features/*/model', capture: ['feature'] },
        { type: 'feature', pattern: 'src/features/*', capture: ['feature'] },
        { type: 'shared-api', pattern: 'src/shared/api' },
        { type: 'shared', pattern: 'src/shared/*' },
        { type: 'app', pattern: 'src/app' },
        { type: 'routes', pattern: 'src/routes' },
        { type: 'test', pattern: 'src/test' },
      ],
      // arquivos soltos na raiz de src (main.tsx, routeTree.gen.ts): categoria, não elemento
      'boundaries/files': [
        { category: 'entry', pattern: 'src/*.{ts,tsx}' },
        { category: 'teste', pattern: '**/*.test.{ts,tsx}' },
        // tipos gerados do contrato: model/ e api/ podem vê-los; o cliente (client.ts) só a api/
        { category: 'tipos-da-api', pattern: 'src/shared/api/schema.d.ts' },
      ],
    },
    rules: {
      'boundaries/dependencies': [
        'error',
        {
          default: 'disallow',
          policies: [
            {
              from: { file: { categories: 'entry' } },
              allow: { to: [el('app'), el('shared'), el('routes')] },
            },
            {
              from: el('app'),
              allow: {
                to: [
                  el('app'),
                  { file: { categories: 'entry' } },
                  el('shared'),
                  el('shared-api'),
                  el('feature'),
                  el('test'),
                ],
              },
            },
            { from: el('routes'), allow: { to: [el('app'), el('routes'), el('shared'), el('feature')] } },
            { from: el('shared'), allow: { to: [el('shared')] } },
            { from: el('shared-api'), allow: { to: [el('shared-api'), el('shared'), TIPOS_DA_API] } },
            {
              from: el('test'),
              allow: { to: [el('test'), el('app'), el('shared'), el('shared-api'), TIPOS_DA_API] },
            },
            ...politicasDasFeatures,
            // testes de qualquer pasta podem usar os utilitários de src/test
            { from: { file: { categories: 'teste' } }, allow: { to: [el('test')] } },
          ],
        },
      ],
    },
  },
)
