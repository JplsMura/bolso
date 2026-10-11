import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { violacoesDeAcessibilidade } from '@/test/axe'
import { BTG, cartoesNaCasa, NUBANK } from '@/test/msw/cartoes'
import {
  CASA_ID,
  CONFORTO,
  CUSTOS_FIXOS,
  handlersDeMetasETags,
  lancamentoDe,
  listaCapturando,
  problema,
  TAG_LUZ,
  urlDoEspaco,
} from '@/test/msw/lancamentos'
import { server } from '@/test/msw/server'
import { renderizarRota } from '@/test/renderizarRota'
import type { ParcelamentoApi } from '../model/tipos'

/** Responde a fatura da compra (mês de março do Nubank) e guarda as URLs pedidas. */
function faturaDaCompraCapturando(status?: 'OPEN' | 'CLOSED' | 'PAID') {
  const urls: URL[] = []
  const handler = http.get(urlDoEspaco(CASA_ID, '/cartoes/:id/fatura-da-compra'), ({ request }) => {
    urls.push(new URL(request.url))
    return HttpResponse.json({
      mes: '2026-03',
      fechamento: '2026-03-25',
      vencimento: '2026-04-05',
      ...(status ? { status } : {}),
    })
  })
  return { handler, urls }
}

/** Valor, descrição, meta, Cartão e data (10/03/2026). Não escolhe o cartão. */
async function preencherGastoNoCartao(usuario: ReturnType<typeof userEvent.setup>, digitos = '15000') {
  await usuario.type(await screen.findByLabelText('Valor'), digitos)
  await usuario.type(screen.getByLabelText('Descrição'), 'Curso online')
  await usuario.click(await screen.findByRole('button', { name: 'Custos Fixos' }))
  fireEvent.change(screen.getByLabelText('Data'), { target: { value: '2026-03-10' } })
  await usuario.click(screen.getByRole('button', { name: 'Cartão' }))
}

describe('gasto no cartão', () => {
  it('Cartão exige escolher um cartão; mostra o disponível e em qual fatura a compra cai', async () => {
    const usuario = userEvent.setup()
    let corpo: unknown
    const fatura = faturaDaCompraCapturando()
    const criado = lancamentoDe({
      id: '019a0000-0000-7000-8000-00000000e001',
      formaPagamento: 'CREDIT',
      cartaoId: NUBANK.id,
    })
    server.use(
      ...handlersDeMetasETags(),
      cartoesNaCasa([NUBANK, BTG]),
      fatura.handler,
      http.post(urlDoEspaco(CASA_ID, '/lancamentos'), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(criado, { status: 201 })
      }),
      listaCapturando([criado]).handler,
    )
    renderizarRota('/casa/lancamentos/novo')
    await preencherGastoNoCartao(usuario)

    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('Escolha o cartão')).toBeInTheDocument()
    expect(corpo).toBeUndefined()

    expect(screen.getByRole('button', { name: 'BTG' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Nubank' }))
    expect((await screen.findByText(/Disponível no Nubank/)).closest('p')).toHaveTextContent(/R\$\s3\.240,00/)
    expect(
      await screen.findByText(/Entra na fatura de março de 2026 do Nubank · fecha 25\/03 · vence 05\/04/),
    ).toBeInTheDocument()
    expect(fatura.urls.at(-1)?.pathname).toContain(`/cartoes/${NUBANK.id}/fatura-da-compra`)
    expect(fatura.urls.at(-1)?.searchParams.get('data')).toBe('2026-03-10')

    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(corpo).toBeDefined())
    expect(corpo).toMatchObject({ formaPagamento: 'CREDIT', cartaoId: NUBANK.id, valor: '150.00' })
  })

  it('avisa quando a fatura da compra já está fechada e quando passa do limite', async () => {
    const usuario = userEvent.setup()
    server.use(...handlersDeMetasETags(), cartoesNaCasa([NUBANK]), faturaDaCompraCapturando('CLOSED').handler)
    renderizarRota('/casa/lancamentos/novo')
    await preencherGastoNoCartao(usuario, '400000') // R$ 4.000,00 > disponível de R$ 3.240,00

    await usuario.click(screen.getByRole('button', { name: /^Nubank/ }))

    expect(await screen.findByText(/A fatura de março de 2026 do Nubank já está fechada/)).toBeInTheDocument()
    expect(screen.getByText(/Passa do limite do Nubank: disponível R\$ 3\.240,00/)).toBeInTheDocument()
  })

  it('sem cartão cadastrado oferece o cadastro', async () => {
    const usuario = userEvent.setup()
    server.use(...handlersDeMetasETags())
    renderizarRota('/casa/lancamentos/novo')

    await usuario.click(await screen.findByRole('button', { name: 'Cartão' }))

    expect(await screen.findByText(/Nenhum cartão cadastrado/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Cadastrar cartão' })).toBeInTheDocument()
  })

  it('parcelado envia o total para /parcelamentos e volta para o mês da compra', async () => {
    const usuario = userEvent.setup()
    let corpo: unknown
    let usouLancamentos = false
    const parcelamento: ParcelamentoApi = {
      id: '019a0000-0000-7000-8000-00000000e100',
      descricao: 'Curso online',
      valorTotal: '150.00',
      parcelas: 6,
      parcelaAtual: 1,
      data: '2026-03-10',
      cartaoId: NUBANK.id,
      metaId: CUSTOS_FIXOS.id,
      tags: [],
      versao: 0,
      itens: [
        {
          lancamentoId: 'p1',
          numero: 1,
          valor: '25.00',
          data: '2026-03-10',
          faturaMes: '2026-03',
          faturaAberta: true,
        },
      ],
    }
    server.use(
      ...handlersDeMetasETags(),
      cartoesNaCasa([NUBANK]),
      faturaDaCompraCapturando().handler,
      http.post(urlDoEspaco(CASA_ID, '/lancamentos'), () => {
        usouLancamentos = true
        return problema(500)
      }),
      http.post(urlDoEspaco(CASA_ID, '/parcelamentos'), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(parcelamento, { status: 201 })
      }),
      listaCapturando([]).handler,
    )
    const { router } = renderizarRota('/casa/lancamentos/novo')
    await preencherGastoNoCartao(usuario)
    await usuario.click(screen.getByRole('button', { name: /^Nubank/ }))

    const parcelas = screen.getByLabelText('Parcelas')
    await usuario.clear(parcelas)
    await usuario.type(parcelas, '6')
    expect(screen.getByText(/6x de R\$ 25,00 \(o valor é o total da compra\)/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Salvar em 6x' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/casa/lancamentos'))
    expect(corpo).toEqual({
      valorTotal: '150.00',
      parcelas: 6,
      descricao: 'Curso online',
      data: '2026-03-10',
      cartaoId: NUBANK.id,
      tagIds: [],
      metaId: CUSTOS_FIXOS.id,
    })
    expect(usouLancamentos).toBe(false)
    expect(router.state.location.search).toMatchObject({ mes: '2026-03' })
  })

  it('compra em andamento manda a parcela atual', async () => {
    const usuario = userEvent.setup()
    let corpo: unknown
    server.use(
      ...handlersDeMetasETags(),
      cartoesNaCasa([NUBANK]),
      faturaDaCompraCapturando().handler,
      http.post(urlDoEspaco(CASA_ID, '/parcelamentos'), async ({ request }) => {
        corpo = await request.json()
        return problema(500)
      }),
    )
    renderizarRota('/casa/lancamentos/novo')
    await preencherGastoNoCartao(usuario)
    await usuario.click(screen.getByRole('button', { name: /^Nubank/ }))
    const parcelas = screen.getByLabelText('Parcelas')
    await usuario.clear(parcelas)
    await usuario.type(parcelas, '6')

    await usuario.click(screen.getByLabelText('Compra já em andamento (de antes do histórico)'))
    const atual = screen.getByLabelText('Parcela atual')
    await usuario.clear(atual)
    await usuario.type(atual, '7')
    await usuario.click(screen.getByRole('button', { name: 'Salvar em 6x' }))
    expect(await screen.findByText('A parcela atual vai de 1 a 6')).toBeInTheDocument()

    await usuario.clear(atual)
    await usuario.type(atual, '4')
    await usuario.click(screen.getByRole('button', { name: 'Salvar em 6x' }))
    await waitFor(() => expect(corpo).toMatchObject({ parcelas: 6, parcelaAtual: 4 }))
  })

  it('fatura fechada (422) mostra o motivo e o atalho para Cartões', async () => {
    const usuario = userEvent.setup()
    server.use(
      ...handlersDeMetasETags(),
      cartoesNaCasa([NUBANK]),
      faturaDaCompraCapturando().handler,
      http.post(urlDoEspaco(CASA_ID, '/lancamentos'), () =>
        HttpResponse.json(
          {
            status: 422,
            type: 'urn:bolso:problema:fatura-fechada',
            detail:
              'A fatura de março/2026 do Nubank já está fechada. Reabra a fatura em Cartões para mudar este gasto.',
          },
          { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    renderizarRota('/casa/lancamentos/novo')
    await preencherGastoNoCartao(usuario)
    await usuario.click(screen.getByRole('button', { name: /^Nubank/ }))

    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))

    const alerta = await screen.findByText(/já está fechada. Reabra a fatura em Cartões/)
    expect(within(alerta).getByRole('link', { name: 'Ir para Cartões' })).toBeInTheDocument()
  })

  it('não tem violações de acessibilidade com cartão e parcelas', async () => {
    const usuario = userEvent.setup()
    server.use(...handlersDeMetasETags(), cartoesNaCasa([NUBANK, BTG]), faturaDaCompraCapturando().handler)
    const { container } = renderizarRota('/casa/lancamentos/novo')
    await preencherGastoNoCartao(usuario)
    await usuario.click(screen.getByRole('button', { name: /^Nubank/ }))
    const parcelas = screen.getByLabelText('Parcelas')
    await usuario.clear(parcelas)
    await usuario.type(parcelas, '3')
    await usuario.click(screen.getByLabelText('Compra já em andamento (de antes do histórico)'))
    await screen.findByText(/Entra na fatura/)

    expect(await violacoesDeAcessibilidade(container)).toEqual([])
  })
})

const PARCELA = lancamentoDe({
  id: '019a0000-0000-7000-8000-00000000e201',
  descricao: 'Curso online',
  valor: '25.00',
  formaPagamento: 'CREDIT',
  cartaoId: NUBANK.id,
  faturaMes: '2026-03',
  metaId: CONFORTO.id,
  tags: [],
  parcela: { parcelamentoId: '019a0000-0000-7000-8000-00000000e100', numero: 2, total: 6 },
})

describe('lista com cartão', () => {
  it('mostra o nome do cartão e a parcela, e filtra por cartão', async () => {
    const usuario = userEvent.setup()
    const { handler, urls } = listaCapturando([PARCELA])
    server.use(...handlersDeMetasETags(), cartoesNaCasa([NUBANK, BTG]), handler)
    renderizarRota('/casa/lancamentos?mes=2026-03')

    expect(await screen.findByText('Conforto · Nubank')).toBeInTheDocument()
    expect(screen.getByText('2/6')).toBeInTheDocument()

    await usuario.selectOptions(screen.getByLabelText('Cartão'), 'BTG')
    await waitFor(() => expect(urls.at(-1)?.searchParams.get('cartaoId')).toBe(BTG.id))
    await usuario.selectOptions(screen.getByLabelText('Pagamento'), 'Cartão')
    await waitFor(() => expect(urls.at(-1)?.searchParams.get('pagamento')).toBe('CREDIT'))
  })

  it('sem cartões, o filtro de cartão não aparece', async () => {
    server.use(...handlersDeMetasETags(), listaCapturando([lancamentoDe({ id: 'x1' })]).handler)
    renderizarRota('/casa/lancamentos?mes=2026-03')

    await screen.findByText('Conta de luz')
    expect(screen.queryByLabelText('Cartão')).not.toBeInTheDocument()
  })
})

const PARCELAMENTO: ParcelamentoApi = {
  id: '019a0000-0000-7000-8000-00000000e100',
  descricao: 'Curso online',
  valorTotal: '150.00',
  parcelas: 6,
  parcelaAtual: 1,
  data: '2026-02-10',
  cartaoId: NUBANK.id,
  metaId: CONFORTO.id,
  tags: [],
  versao: 3,
  itens: [
    {
      lancamentoId: 'p1',
      numero: 1,
      valor: '25.00',
      data: '2026-02-10',
      faturaMes: '2026-02',
      faturaAberta: false,
    },
    {
      lancamentoId: '019a0000-0000-7000-8000-00000000e201',
      numero: 2,
      valor: '25.00',
      data: '2026-03-10',
      faturaMes: '2026-03',
      faturaAberta: true,
    },
    {
      lancamentoId: 'p3',
      numero: 3,
      valor: '25.00',
      data: '2026-04-10',
      faturaMes: '2026-04',
      faturaAberta: true,
    },
  ],
}
const URL_PARCELAMENTO = urlDoEspaco(CASA_ID, `/parcelamentos/${PARCELAMENTO.id}`)

describe('parcelamento', () => {
  it('editar uma parcela leva ao parcelamento', async () => {
    const usuario = userEvent.setup()
    server.use(
      ...handlersDeMetasETags(),
      cartoesNaCasa([NUBANK]),
      http.get(urlDoEspaco(CASA_ID, `/lancamentos/${PARCELA.id}`), () => HttpResponse.json(PARCELA)),
      http.get(URL_PARCELAMENTO, () => HttpResponse.json(PARCELAMENTO)),
    )
    const { router } = renderizarRota(`/casa/lancamentos/${PARCELA.id}`)

    expect(await screen.findByText(/é a parcela 2\/6 de uma compra parcelada/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Valor')).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('link', { name: 'Abrir o parcelamento' }))

    await waitFor(() => expect(router.state.location.pathname).toBe(`/casa/parcelamentos/${PARCELAMENTO.id}`))
    expect(await screen.findByText('R$ 150,00 em 6x no Nubank · compra em 10/02/2026')).toBeInTheDocument()
  })

  it('mostra as parcelas e o PUT leva descrição, meta, tags e a versão', async () => {
    const usuario = userEvent.setup()
    let corpo: unknown
    server.use(
      ...handlersDeMetasETags(),
      cartoesNaCasa([NUBANK]),
      http.get(URL_PARCELAMENTO, () => HttpResponse.json(PARCELAMENTO)),
      http.put(URL_PARCELAMENTO, async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json({ ...PARCELAMENTO, descricao: 'Curso de Java', versao: 4 })
      }),
    )
    renderizarRota(`/casa/parcelamentos/${PARCELAMENTO.id}`)

    const parcelas = await screen.findByRole('region', { name: 'Parcelas' })
    expect(within(parcelas).getAllByRole('listitem')).toHaveLength(3)
    expect(within(parcelas).getByText('Fatura Fev 2026 · fechada ou paga')).toBeInTheDocument()
    expect(screen.getByText('As mudanças valem para as 2 parcelas em faturas abertas.')).toBeInTheDocument()

    const descricao = screen.getByLabelText('Descrição')
    await usuario.clear(descricao)
    await usuario.type(descricao, 'Curso de Java')
    await usuario.click(await screen.findByRole('button', { name: 'Custos Fixos' }))
    await usuario.click(screen.getByRole('combobox', { name: 'Tags' }))
    await usuario.click(await screen.findByRole('option', { name: 'Luz' }))
    await usuario.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    await waitFor(() =>
      expect(corpo).toEqual({
        descricao: 'Curso de Java',
        metaId: CUSTOS_FIXOS.id,
        tagIds: [TAG_LUZ.id],
        versao: 3,
      }),
    )
  })

  it('excluir as parcelas restantes pede confirmação e volta para a lista', async () => {
    const usuario = userEvent.setup()
    let excluiu = false
    server.use(
      ...handlersDeMetasETags(),
      cartoesNaCasa([NUBANK]),
      http.get(URL_PARCELAMENTO, () => HttpResponse.json(PARCELAMENTO)),
      http.delete(URL_PARCELAMENTO, () => {
        excluiu = true
        return new HttpResponse(null, { status: 204 })
      }),
      listaCapturando([]).handler,
    )
    const { router, container } = renderizarRota(`/casa/parcelamentos/${PARCELAMENTO.id}`)
    await screen.findByLabelText('Descrição')
    expect(await violacoesDeAcessibilidade(container)).toEqual([])

    await usuario.click(screen.getByRole('button', { name: 'Excluir parcelas restantes' }))
    const confirmacao = await screen.findByRole('alertdialog', { name: 'Excluir as parcelas restantes?' })
    expect(confirmacao).toHaveTextContent('Saem as 2 parcelas')
    expect(await violacoesDeAcessibilidade(container)).toEqual([])
    await usuario.click(within(confirmacao).getByRole('button', { name: 'Excluir' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/casa/lancamentos'))
    expect(excluiu).toBe(true)
    expect(router.state.location.search).toMatchObject({ mes: '2026-02' })
  })

  it('sem parcela em fatura aberta, não oferece edição', async () => {
    const todasFechadas = {
      ...PARCELAMENTO,
      itens: PARCELAMENTO.itens.map((i) => ({ ...i, faturaAberta: false })),
    }
    server.use(
      ...handlersDeMetasETags(),
      cartoesNaCasa([NUBANK]),
      http.get(URL_PARCELAMENTO, () => HttpResponse.json(todasFechadas)),
    )
    renderizarRota(`/casa/parcelamentos/${PARCELAMENTO.id}`)

    expect(
      await screen.findByText(/Todas as parcelas que sobram estão em faturas fechadas ou pagas/),
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Salvar alterações' })).not.toBeInTheDocument()
  })
})
