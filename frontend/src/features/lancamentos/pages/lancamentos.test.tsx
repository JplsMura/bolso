import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '@/test/msw/server'
import {
  CASA_ID,
  CONFORTO,
  CUSTOS_FIXOS,
  EMPRESA_ID,
  handlersDeMetasETags,
  lancamentoDe,
  listaCapturando,
  listaDoMes,
  problema,
  TAG_LUZ,
  TAG_UBER,
  urlDoEspaco,
} from '@/test/msw/lancamentos'
import { renderizarRota } from '@/test/renderizarRota'
import { violacoesDeAcessibilidade } from '@/test/axe'
import type { LancamentoApi } from '../model/tipos'

/** Entrada: sem meta e sem forma de pagamento (campos ausentes, como a API manda). */
const SALARIO: LancamentoApi = {
  id: '019a0000-0000-7000-8000-00000000a001',
  direcao: 'IN',
  valor: '5000.00',
  descricao: 'Salário',
  data: '2026-03-05',
  mesReferencia: '2026-03',
  tags: [],
  versao: 0,
}
const LUZ = lancamentoDe({ id: '019a0000-0000-7000-8000-00000000a002', data: '2026-03-10' })
const UBER = lancamentoDe({
  id: '019a0000-0000-7000-8000-00000000a003',
  valor: '23.90',
  descricao: 'Uber',
  data: '2026-03-10',
  metaId: CONFORTO.id,
  formaPagamento: 'DEBIT',
  tags: [TAG_UBER],
})
const ITENS = [UBER, LUZ, SALARIO]

describe('lista de lançamentos', () => {
  it('mostra os itens agrupados por dia, os totais do mês e os detalhes', async () => {
    const { handler } = listaCapturando(ITENS)
    server.use(...handlersDeMetasETags(), handler)

    renderizarRota('/casa/lancamentos?mes=2026-03')

    const dia10 = await screen.findByRole('region', { name: '10 de março' })
    expect(within(dia10).getByText('Uber')).toBeInTheDocument()
    expect(within(dia10).getByText('Custos Fixos · Luz · PIX')).toBeInTheDocument()
    expect(within(dia10).getByText('Conforto · Uber · Débito')).toBeInTheDocument()
    expect(within(dia10).getByText('− R$ 110,00')).toBeInTheDocument()
    const dia5 = screen.getByRole('region', { name: '5 de março' })
    expect(within(dia5).getByText('Entrada')).toBeInTheDocument()
    expect(within(dia5).getByText('+ R$ 5.000,00')).toBeInTheDocument()

    expect(screen.getByText('Entradas', { selector: 'dt' }).nextElementSibling).toHaveTextContent(
      'R$ 5.000,00',
    )
    expect(screen.getByText('Saídas', { selector: 'dt' }).nextElementSibling).toHaveTextContent('R$ 133,90')
    expect(screen.getByText('Sobra', { selector: 'dt' }).nextElementSibling).toHaveTextContent('R$ 4.866,10')
    expect(screen.getByText('Mar 2026')).toBeInTheDocument()
  })

  it('mês sem lançamentos mostra o estado vazio e totais zerados', async () => {
    server.use(...handlersDeMetasETags(), listaCapturando([]).handler)

    renderizarRota('/casa/lancamentos?mes=2026-03')

    expect(await screen.findByText('Nenhum lançamento em março de 2026.')).toBeInTheDocument()
    expect(screen.getByText('Sobra', { selector: 'dt' }).nextElementSibling).toHaveTextContent('R$ 0,00')
  })

  it('trocar de mês refaz a consulta com o mês novo', async () => {
    const usuario = userEvent.setup()
    const { handler, urls } = listaCapturando(ITENS)
    server.use(...handlersDeMetasETags(), handler)
    const { router } = renderizarRota('/casa/lancamentos?mes=2026-03')
    await screen.findByText('Mar 2026')

    await usuario.click(screen.getByRole('button', { name: 'Próximo mês' }))

    expect(await screen.findByText('Abr 2026')).toBeInTheDocument()
    await waitFor(() => expect(urls.at(-1)?.searchParams.get('mes')).toBe('2026-04'))
    expect(router.state.location.search).toMatchObject({ mes: '2026-04' })

    await usuario.click(screen.getByRole('button', { name: 'Mês anterior' }))
    await usuario.click(screen.getByRole('button', { name: 'Mês anterior' }))
    expect(await screen.findByText('Fev 2026')).toBeInTheDocument()
  })

  it('busca e filtros mandam os parâmetros certos', async () => {
    const usuario = userEvent.setup()
    const { handler, urls } = listaCapturando(ITENS)
    server.use(...handlersDeMetasETags(), handler)
    renderizarRota('/casa/lancamentos?mes=2026-03')
    await screen.findByText('Salário')
    const ultima = () => urls.at(-1)?.searchParams

    await usuario.type(screen.getByLabelText('Buscar lançamentos'), 'uber')
    await waitFor(() => expect(ultima()?.get('q')).toBe('uber'))

    await usuario.selectOptions(screen.getByLabelText('Meta'), 'Conforto')
    await waitFor(() => expect(ultima()?.get('metaId')).toBe(CONFORTO.id))

    await usuario.selectOptions(screen.getByLabelText('Tag'), 'Uber')
    await waitFor(() => expect(ultima()?.get('tagId')).toBe(TAG_UBER.id))

    await usuario.selectOptions(screen.getByLabelText('Pagamento'), 'Débito')
    await waitFor(() => expect(ultima()?.get('pagamento')).toBe('DEBIT'))

    await usuario.selectOptions(screen.getByLabelText('Tipo'), 'Saídas')
    await waitFor(() => expect(ultima()?.get('direcao')).toBe('OUT'))
    expect(screen.getByRole('button', { name: 'Todos' })).toHaveAttribute('aria-pressed', 'false')

    await usuario.click(screen.getByRole('button', { name: 'Todos' }))
    await waitFor(() => {
      const busca = ultima()
      expect(busca?.get('mes')).toBe('2026-03')
      for (const nome of ['q', 'metaId', 'tagId', 'pagamento', 'direcao'])
        expect(busca?.has(nome)).toBe(false)
    })
  })

  it('filtro sem resultado diz que nada foi encontrado', async () => {
    const usuario = userEvent.setup()
    server.use(
      ...handlersDeMetasETags(),
      http.get(urlDoEspaco(CASA_ID, '/lancamentos'), ({ request }) => {
        const filtrando = new URL(request.url).searchParams.has('q')
        return HttpResponse.json(listaDoMes('2026-03', filtrando ? [] : ITENS))
      }),
    )
    renderizarRota('/casa/lancamentos?mes=2026-03')
    await screen.findByText('Salário')

    await usuario.type(screen.getByLabelText('Buscar lançamentos'), 'xyz')

    expect(await screen.findByText('Nenhum lançamento encontrado com esses filtros.')).toBeInTheDocument()
  })

  it('erro ao carregar mostra o alerta e "Tentar de novo" repete a consulta', async () => {
    const usuario = userEvent.setup()
    let tentativas = 0
    server.use(
      ...handlersDeMetasETags(),
      http.get(urlDoEspaco(CASA_ID, '/lancamentos'), () => {
        tentativas += 1
        return tentativas === 1 ? problema(500) : HttpResponse.json(listaDoMes('2026-03', ITENS))
      }),
    )
    renderizarRota('/casa/lancamentos?mes=2026-03')

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar os lançamentos.')
    await usuario.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByText('Salário')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('o item Lançamentos do menu da Casa leva para a lista; a Empresa não tem o item', async () => {
    const usuario = userEvent.setup()
    server.use(...handlersDeMetasETags(), listaCapturando(ITENS).handler)
    const { router } = renderizarRota('/casa')
    const menu = await screen.findByRole('navigation', { name: 'Principal' })

    await usuario.click(within(menu).getByRole('link', { name: 'Lançamentos' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/casa/lancamentos'))
    expect(await screen.findByRole('heading', { name: 'Lançamentos' })).toBeInTheDocument()
  })

  it('ao trocar de espaço não mostra a lista do espaço anterior', async () => {
    server.use(
      ...handlersDeMetasETags(),
      listaCapturando(ITENS).handler,
      http.get(urlDoEspaco(EMPRESA_ID, '/tags'), () => HttpResponse.json([])),
      http.get(urlDoEspaco(EMPRESA_ID, '/lancamentos'), async () => {
        await delay(300)
        return HttpResponse.json(listaDoMes('2026-03', []))
      }),
    )
    const { router } = renderizarRota('/casa/lancamentos?mes=2026-03')
    await screen.findByText('Salário')

    await router.navigate({
      to: '/$espaco/lancamentos',
      params: { espaco: 'empresa' },
      search: { mes: '2026-03' },
    })

    expect(await screen.findByText('Carregando lançamentos…')).toBeInTheDocument()
    expect(screen.queryByText('Salário')).not.toBeInTheDocument()
    expect(await screen.findByText('Nenhum lançamento em março de 2026.')).toBeInTheDocument()
  })

  it('não tem violações de acessibilidade', async () => {
    server.use(...handlersDeMetasETags(), listaCapturando(ITENS).handler)
    const { container } = renderizarRota('/casa/lancamentos?mes=2026-03')
    await screen.findByText('Salário')

    expect(await violacoesDeAcessibilidade(container)).toEqual([])
  })
})

/** Preenche o gasto da Casa e deixa o botão Salvar pronto para clicar. */
async function preencherGasto(usuario: ReturnType<typeof userEvent.setup>) {
  await usuario.type(await screen.findByLabelText('Valor'), '11000')
  await usuario.type(screen.getByLabelText('Descrição'), 'Conta de luz')
  await usuario.click(await screen.findByRole('button', { name: 'Custos Fixos' }))
  await usuario.click(await screen.findByRole('button', { name: 'Luz' }))
  await usuario.click(screen.getByRole('button', { name: 'PIX' }))
  fireEvent.change(screen.getByLabelText('Data'), { target: { value: '2026-03-10' } })
}

describe('novo lançamento', () => {
  it('valida valor, descrição, meta e pagamento antes de enviar', async () => {
    const usuario = userEvent.setup()
    let enviou = false
    server.use(
      ...handlersDeMetasETags(),
      http.post(urlDoEspaco(CASA_ID, '/lancamentos'), () => {
        enviou = true
        return problema(500)
      }),
    )
    renderizarRota('/casa/lancamentos/novo')
    await screen.findByRole('button', { name: 'Custos Fixos' })

    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Informe um valor maior que zero')).toBeInTheDocument()
    expect(screen.getByText('Informe a descrição')).toBeInTheDocument()
    expect(screen.getByText('Escolha a meta do gasto')).toBeInTheDocument()
    expect(screen.getByText('Escolha a forma de pagamento')).toBeInTheDocument()
    expect(enviou).toBe(false)
  })

  it('envia o valor como texto da API e volta para o mês do lançamento', async () => {
    const usuario = userEvent.setup()
    let corpo: unknown
    const criado = lancamentoDe({ id: '019a0000-0000-7000-8000-00000000a010' })
    server.use(
      ...handlersDeMetasETags(),
      http.post(urlDoEspaco(CASA_ID, '/lancamentos'), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(criado, { status: 201 })
      }),
      listaCapturando([criado]).handler,
    )
    const { router } = renderizarRota('/casa/lancamentos/novo')

    await preencherGasto(usuario)
    expect(screen.getByLabelText('Valor')).toHaveValue('R$\u00a0110,00')
    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/casa/lancamentos'))
    expect(corpo).toEqual({
      direcao: 'OUT',
      valor: '110.00',
      descricao: 'Conta de luz',
      data: '2026-03-10',
      metaId: CUSTOS_FIXOS.id,
      formaPagamento: 'PIX',
      tagIds: [TAG_LUZ.id],
    })
    expect(router.state.location.search).toMatchObject({ mes: '2026-03' })
    expect(await screen.findByText('Conta de luz')).toBeInTheDocument()
  })

  it('entrada: só valor, descrição e data, sem meta nem forma de pagamento', async () => {
    const usuario = userEvent.setup()
    let corpo: unknown
    const criado = lancamentoDe({
      id: '019a0000-0000-7000-8000-00000000a011',
      direcao: 'IN',
      descricao: 'Salário',
    })
    server.use(
      ...handlersDeMetasETags(),
      http.post(urlDoEspaco(CASA_ID, '/lancamentos'), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(criado, { status: 201 })
      }),
      listaCapturando([criado]).handler,
    )
    renderizarRota('/casa/lancamentos/novo?tipo=entrada')

    await usuario.type(await screen.findByLabelText('Valor'), '500000')
    await usuario.type(screen.getByLabelText('Descrição'), 'Salário')
    fireEvent.change(screen.getByLabelText('Data'), { target: { value: '2026-03-05' } })
    expect(screen.queryByRole('button', { name: 'Custos Fixos' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'PIX' })).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(corpo).toBeDefined())
    expect(corpo).toEqual({
      direcao: 'IN',
      valor: '5000.00',
      descricao: 'Salário',
      data: '2026-03-05',
      tagIds: [],
    })
  })

  it('trocar para Entrada limpa a meta e o pagamento já escolhidos', async () => {
    const usuario = userEvent.setup()
    let corpo: unknown
    const criado = lancamentoDe({ id: '019a0000-0000-7000-8000-00000000a012', direcao: 'IN' })
    server.use(
      ...handlersDeMetasETags(),
      http.post(urlDoEspaco(CASA_ID, '/lancamentos'), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(criado, { status: 201 })
      }),
      listaCapturando([criado]).handler,
    )
    renderizarRota('/casa/lancamentos/novo')
    await preencherGasto(usuario)

    await usuario.click(screen.getByRole('button', { name: 'Entrada' }))
    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(corpo).toBeDefined())
    expect(corpo).not.toHaveProperty('metaId')
    expect(corpo).not.toHaveProperty('formaPagamento')
  })

  it('cria uma tag no próprio formulário e já a marca', async () => {
    const usuario = userEvent.setup()
    let corpoTag: unknown
    const nova = { id: '019a0000-0000-7000-8000-0000000000b3', nome: 'Mercado' }
    let tagsCriadas = false
    server.use(
      http.get(urlDoEspaco(CASA_ID, '/metas'), () => HttpResponse.json([CUSTOS_FIXOS])),
      http.get(urlDoEspaco(CASA_ID, '/tags'), () =>
        HttpResponse.json(tagsCriadas ? [TAG_LUZ, nova] : [TAG_LUZ]),
      ),
      http.post(urlDoEspaco(CASA_ID, '/tags'), async ({ request }) => {
        corpoTag = await request.json()
        tagsCriadas = true
        return HttpResponse.json(nova, { status: 201 })
      }),
    )
    renderizarRota('/casa/lancamentos/novo')

    await usuario.type(await screen.findByLabelText('Nova tag'), 'Mercado{Enter}')

    expect(await screen.findByRole('button', { name: 'Mercado', pressed: true })).toBeInTheDocument()
    expect(corpoTag).toEqual({ nome: 'Mercado' })
  })

  it('mostra o motivo quando o servidor recusa (422)', async () => {
    const usuario = userEvent.setup()
    server.use(
      ...handlersDeMetasETags(),
      http.post(urlDoEspaco(CASA_ID, '/lancamentos'), () =>
        problema(422, 'Cartão de crédito chega com a feature de cartões'),
      ),
    )
    renderizarRota('/casa/lancamentos/novo')
    await preencherGasto(usuario)

    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Cartão de crédito chega com a feature de cartões')).toBeInTheDocument()
  })

  it('na Empresa não pede meta e nem consulta as metas', async () => {
    const usuario = userEvent.setup()
    let corpo: unknown
    const criado = lancamentoDe({ id: '019a0000-0000-7000-8000-00000000a013' })
    server.use(
      http.get(urlDoEspaco(EMPRESA_ID, '/tags'), () => HttpResponse.json([])),
      http.post(urlDoEspaco(EMPRESA_ID, '/lancamentos'), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(criado, { status: 201 })
      }),
      listaCapturando([criado], EMPRESA_ID).handler,
    )
    renderizarRota('/empresa/lancamentos/novo')

    await usuario.type(await screen.findByLabelText('Valor'), '30000')
    await usuario.type(screen.getByLabelText('Descrição'), 'Nota fiscal')
    await usuario.click(screen.getByRole('button', { name: 'Boleto' }))
    expect(screen.queryByText('Meta')).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(corpo).toBeDefined())
    expect(corpo).toMatchObject({ direcao: 'OUT', valor: '300.00', formaPagamento: 'BOLETO' })
    expect(corpo).not.toHaveProperty('metaId')
  })

  it('não tem violações de acessibilidade (limpo e com erros de validação)', async () => {
    const usuario = userEvent.setup()
    server.use(...handlersDeMetasETags())
    const { container } = renderizarRota('/casa/lancamentos/novo')
    await screen.findByRole('button', { name: 'Custos Fixos' })
    expect(await violacoesDeAcessibilidade(container)).toEqual([])

    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))
    await screen.findByText('Informe a descrição')
    expect(await violacoesDeAcessibilidade(container)).toEqual([])
  })
})

const EXISTENTE = lancamentoDe({ id: '019a0000-0000-7000-8000-00000000a020', versao: 3 })
const URL_EXISTENTE = urlDoEspaco(CASA_ID, `/lancamentos/${EXISTENTE.id}`)

describe('editar e excluir', () => {
  it('abre preenchido e o PUT leva a versão lida', async () => {
    const usuario = userEvent.setup()
    let corpo: unknown
    server.use(
      ...handlersDeMetasETags(),
      http.get(URL_EXISTENTE, () => HttpResponse.json(EXISTENTE)),
      http.put(URL_EXISTENTE, async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json({ ...EXISTENTE, descricao: 'Luz de março', versao: 4 })
      }),
      listaCapturando([EXISTENTE]).handler,
    )
    const { router } = renderizarRota(`/casa/lancamentos/${EXISTENTE.id}`)

    const descricao = await screen.findByLabelText('Descrição')
    expect(descricao).toHaveValue('Conta de luz')
    expect(screen.getByLabelText('Valor')).toHaveValue('R$\u00a0110,00')
    expect(await screen.findByRole('button', { name: 'Custos Fixos', pressed: true })).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Luz', pressed: true })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'PIX', pressed: true })).toBeInTheDocument()

    await usuario.clear(descricao)
    await usuario.type(descricao, 'Luz de março')
    await usuario.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/casa/lancamentos'))
    expect(corpo).toMatchObject({
      descricao: 'Luz de março',
      valor: '110.00',
      versao: 3,
      metaId: CUSTOS_FIXOS.id,
    })
  })

  it('versão desatualizada (409) pede para recarregar e traz a versão nova', async () => {
    const usuario = userEvent.setup()
    let leituras = 0
    server.use(
      ...handlersDeMetasETags(),
      http.get(URL_EXISTENTE, () => {
        leituras += 1
        return HttpResponse.json(
          leituras === 1 ? EXISTENTE : { ...EXISTENTE, descricao: 'Alterada por outro', versao: 4 },
        )
      }),
      http.put(URL_EXISTENTE, () => problema(409)),
    )
    renderizarRota(`/casa/lancamentos/${EXISTENTE.id}`)
    await screen.findByLabelText('Descrição')

    await usuario.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('Alguém alterou este lançamento')
    await usuario.click(within(alerta).getByRole('button', { name: 'Recarregar' }))

    await waitFor(() => expect(screen.getByLabelText('Descrição')).toHaveValue('Alterada por outro'))
    expect(screen.queryByText(/Alguém alterou este lançamento/)).not.toBeInTheDocument()
  })

  it('excluir pede confirmação; cancelar não exclui', async () => {
    const usuario = userEvent.setup()
    let excluiu = false
    server.use(
      ...handlersDeMetasETags(),
      http.get(URL_EXISTENTE, () => HttpResponse.json(EXISTENTE)),
      http.delete(URL_EXISTENTE, () => {
        excluiu = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderizarRota(`/casa/lancamentos/${EXISTENTE.id}`)
    await screen.findByLabelText('Descrição')

    await usuario.click(screen.getByRole('button', { name: 'Excluir' }))
    const confirmacao = await screen.findByRole('alertdialog', { name: 'Excluir este lançamento?' })
    expect(within(confirmacao).getByRole('button', { name: 'Cancelar' })).toHaveFocus()
    await usuario.click(within(confirmacao).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(excluiu).toBe(false)
  })

  it('confirmar exclui e volta para a lista do mês', async () => {
    const usuario = userEvent.setup()
    let excluiu = false
    server.use(
      ...handlersDeMetasETags(),
      http.get(URL_EXISTENTE, () => HttpResponse.json(EXISTENTE)),
      http.delete(URL_EXISTENTE, () => {
        excluiu = true
        return new HttpResponse(null, { status: 204 })
      }),
      listaCapturando([]).handler,
    )
    const { router } = renderizarRota(`/casa/lancamentos/${EXISTENTE.id}`)
    await screen.findByLabelText('Descrição')

    await usuario.click(screen.getByRole('button', { name: 'Excluir' }))
    const confirmacao = await screen.findByRole('alertdialog')
    await usuario.click(within(confirmacao).getByRole('button', { name: 'Excluir' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/casa/lancamentos'))
    expect(excluiu).toBe(true)
    expect(router.state.location.search).toMatchObject({ mes: '2026-03' })
  })

  it('lançamento que não existe mostra a mensagem e o caminho de volta', async () => {
    server.use(
      ...handlersDeMetasETags(),
      http.get(URL_EXISTENTE, () => problema(404)),
    )
    renderizarRota(`/casa/lancamentos/${EXISTENTE.id}`)

    expect(await screen.findByRole('alert')).toHaveTextContent('Lançamento não encontrado')
    expect(screen.getByRole('link', { name: 'Voltar para a lista' })).toBeInTheDocument()
  })

  it('não tem violações de acessibilidade (formulário e confirmação de exclusão)', async () => {
    const usuario = userEvent.setup()
    server.use(
      ...handlersDeMetasETags(),
      http.get(URL_EXISTENTE, () => HttpResponse.json(EXISTENTE)),
    )
    const { container } = renderizarRota(`/casa/lancamentos/${EXISTENTE.id}`)
    await screen.findByRole('button', { name: 'Custos Fixos', pressed: true })
    expect(await violacoesDeAcessibilidade(container)).toEqual([])

    await usuario.click(screen.getByRole('button', { name: 'Excluir' }))
    await screen.findByRole('alertdialog')
    expect(await violacoesDeAcessibilidade(container)).toEqual([])
  })
})
