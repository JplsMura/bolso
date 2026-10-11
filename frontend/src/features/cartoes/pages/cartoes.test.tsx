import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { violacoesDeAcessibilidade } from '@/test/axe'
import { BTG, cartoesNaCasa, FATURA_NUBANK, faturaDe, faturasCapturando, NUBANK } from '@/test/msw/cartoes'
import { CASA_ID, handlersDeMetasETags, problema, urlDoEspaco } from '@/test/msw/lancamentos'
import { server } from '@/test/msw/server'
import { renderizarRota } from '@/test/renderizarRota'

// "hoje" fixo: depois do fechamento de março (25/03) e antes do vencimento (05/04)
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 2, 28, 12))
})
afterEach(() => vi.useRealTimers())

const FECHADA = faturaDe({ id: '019a0000-0000-7000-8000-0000000000f2', status: 'CLOSED', versao: 1 })
const PAGA = faturaDe({
  id: '019a0000-0000-7000-8000-0000000000f3',
  status: 'PAID',
  pagaEm: '2026-03-28',
  versao: 2,
})

const urlDaFatura = (id: string, acao: string) => urlDoEspaco(CASA_ID, `/faturas/${id}/${acao}`)

describe('tela Cartões', () => {
  it('mostra cada cartão com limite, a fatura do mês, os gastos e a ação do status', async () => {
    server.use(
      ...handlersDeMetasETags(),
      faturasCapturando([
        { cartao: NUBANK, fatura: FATURA_NUBANK },
        { cartao: BTG, fatura: FECHADA },
      ]).handler,
    )
    renderizarRota('/casa/cartoes?mes=2026-03')

    const nubank = await screen.findByRole('region', { name: 'Nubank' })
    expect(within(nubank).getByText('Fecha dia 25 · vence dia 5')).toBeInTheDocument()
    expect(within(nubank).getByText('R$ 1.260,00 de R$ 4.500,00')).toBeInTheDocument()
    expect(within(nubank).getByRole('meter', { name: 'Limite usado do Nubank' })).toHaveAttribute(
      'aria-valuenow',
      '28',
    )
    expect(within(nubank).getByText('R$ 3.240,00')).toBeInTheDocument()
    expect(within(nubank).getByText('Fatura de março de 2026')).toBeInTheDocument()
    expect(within(nubank).getByText('Aberta')).toHaveAttribute('aria-current', 'step')
    expect(within(nubank).getByText('Fecha 25/03 · vence 05/04')).toBeInTheDocument()
    expect(within(nubank).getByText(/Já passou do fechamento/)).toBeInTheDocument()
    const gastos = within(nubank).getByRole('list', { name: 'Gastos da fatura de março de 2026' })
    expect(within(gastos).getAllByRole('listitem')).toHaveLength(3)
    expect(within(gastos).getByText('2/6')).toBeInTheDocument()
    expect(within(gastos).getByText('03/03 · Custos Fixos')).toBeInTheDocument()
    expect(within(nubank).getByRole('button', { name: 'Fechar fatura' })).toBeInTheDocument()

    const btg = screen.getByRole('region', { name: 'BTG' })
    expect(within(btg).getByText('Fechada')).toHaveAttribute('aria-current', 'step')
    expect(within(btg).getByText('Total da fatura')).toBeInTheDocument()
    expect(within(btg).getByRole('button', { name: 'Marcar como paga' })).toBeInTheDocument()
    expect(within(btg).getByRole('button', { name: 'Reabrir fatura' })).toBeInTheDocument()
    expect(screen.getByText(/Pagar a fatura não cria um novo gasto/)).toBeInTheDocument()
  })

  it('fatura aberta antes do fechamento não oferece fechar; paga só oferece desfazer', async () => {
    const futura = faturaDe({
      id: '019a0000-0000-7000-8000-0000000000f4',
      fechamento: '2026-04-25',
      vencimento: '2026-05-05',
      mes: '2026-04',
    })
    server.use(
      ...handlersDeMetasETags(),
      faturasCapturando([
        { cartao: NUBANK, fatura: futura },
        { cartao: BTG, fatura: PAGA },
      ]).handler,
    )
    renderizarRota('/casa/cartoes?mes=2026-04')

    const nubank = await screen.findByRole('region', { name: 'Nubank' })
    expect(within(nubank).queryByRole('button', { name: 'Fechar fatura' })).not.toBeInTheDocument()
    expect(within(nubank).getByText('Total até agora')).toBeInTheDocument()
    const btg = screen.getByRole('region', { name: 'BTG' })
    expect(within(btg).getByText('Paga')).toHaveAttribute('aria-current', 'step')
    expect(within(btg).getByText(/paga em 28\/03/)).toBeInTheDocument()
    expect(within(btg).getByRole('button', { name: 'Desfazer pagamento' })).toBeInTheDocument()
    expect(within(btg).queryByRole('button', { name: 'Marcar como paga' })).not.toBeInTheDocument()
  })

  it('cartão sem compra no mês e cartão que passou do limite', async () => {
    const estourado = { ...BTG, usado: '8100.00', disponivel: '-100.00' }
    server.use(...handlersDeMetasETags(), faturasCapturando([{ cartao: estourado }]).handler)
    renderizarRota('/casa/cartoes?mes=2026-03')

    const btg = await screen.findByRole('region', { name: 'BTG' })
    expect(within(btg).getByText('Sem gastos na fatura de março de 2026.')).toBeInTheDocument()
    expect(within(btg).getByText(/−R\$ 100,00|-R\$ 100,00/)).toHaveTextContent('(passou do limite)')
    expect(within(btg).getByRole('meter')).toHaveAttribute('aria-valuenow', '100')
  })

  it('sem cartões mostra o convite para cadastrar', async () => {
    server.use(...handlersDeMetasETags(), faturasCapturando([]).handler)
    renderizarRota('/casa/cartoes?mes=2026-03')

    expect(await screen.findByText(/Cadastre seu primeiro cartão/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Novo cartão' })).toBeInTheDocument()
  })

  it('trocar de mês refaz a consulta com o mês novo', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const { handler, urls } = faturasCapturando([{ cartao: NUBANK, fatura: FATURA_NUBANK }])
    server.use(...handlersDeMetasETags(), handler)
    const { router } = renderizarRota('/casa/cartoes?mes=2026-03')
    await screen.findByRole('region', { name: 'Nubank' })

    await usuario.click(screen.getByRole('button', { name: 'Próximo mês' }))

    await waitFor(() => expect(urls.at(-1)?.searchParams.get('mes')).toBe('2026-04'))
    expect(router.state.location.search).toMatchObject({ mes: '2026-04' })
  })

  it('erro ao carregar mostra o alerta e "Tentar de novo" repete a consulta', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    let tentativas = 0
    server.use(
      ...handlersDeMetasETags(),
      http.get(urlDoEspaco(CASA_ID, '/faturas'), () => {
        tentativas += 1
        return tentativas === 1
          ? problema(500)
          : HttpResponse.json({ mes: '2026-03', faturas: [{ cartao: NUBANK }] })
      }),
    )
    renderizarRota('/casa/cartoes?mes=2026-03')

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar os cartões.')
    await usuario.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByRole('region', { name: 'Nubank' })).toBeInTheDocument()
  })

  it('o item Cartões do menu da Casa leva para a tela', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    server.use(...handlersDeMetasETags(), faturasCapturando([]).handler)
    const { router } = renderizarRota('/casa')
    const menu = await screen.findByRole('navigation', { name: 'Principal' })

    await usuario.click(within(menu).getByRole('link', { name: 'Cartões' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/casa/cartoes'))
    expect(await screen.findByRole('heading', { name: 'Cartões', level: 1 })).toBeInTheDocument()
  })

  it('não tem violações de acessibilidade', async () => {
    server.use(
      ...handlersDeMetasETags(),
      faturasCapturando([
        { cartao: NUBANK, fatura: FATURA_NUBANK },
        { cartao: BTG, fatura: FECHADA },
      ]).handler,
    )
    const { container } = renderizarRota('/casa/cartoes?mes=2026-03')
    await screen.findByRole('region', { name: 'Nubank' })

    expect(await violacoesDeAcessibilidade(container)).toEqual([])
  })
})

describe('mudar o status da fatura', () => {
  it('fechar manda a versão lida e a tela relê as faturas', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    let corpo: unknown
    const { handler, urls } = faturasCapturando([{ cartao: NUBANK, fatura: FATURA_NUBANK }])
    server.use(
      ...handlersDeMetasETags(),
      handler,
      http.post(urlDaFatura(FATURA_NUBANK.id, 'fechar'), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json({
          cartao: NUBANK,
          fatura: { ...FATURA_NUBANK, status: 'CLOSED', versao: 1 },
        })
      }),
    )
    renderizarRota('/casa/cartoes?mes=2026-03')
    const leiturasAntes = await waitFor(() => {
      expect(urls.length).toBeGreaterThan(0)
      return urls.length
    })

    await usuario.click(await screen.findByRole('button', { name: 'Fechar fatura' }))

    await waitFor(() => expect(corpo).toEqual({ versao: 0 }))
    await waitFor(() => expect(urls.length).toBeGreaterThan(leiturasAntes))
  })

  it('pagar pede a data (hoje por padrão) e manda junto com a versão', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    let corpo: unknown
    server.use(
      ...handlersDeMetasETags(),
      faturasCapturando([{ cartao: BTG, fatura: FECHADA }]).handler,
      http.post(urlDaFatura(FECHADA.id, 'pagar'), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json({ cartao: BTG, fatura: { ...FECHADA, status: 'PAID', versao: 2 } })
      }),
    )
    renderizarRota('/casa/cartoes?mes=2026-03')

    await usuario.click(await screen.findByRole('button', { name: 'Marcar como paga' }))
    expect(screen.getByLabelText('Data do pagamento')).toHaveValue('2026-03-28')
    await usuario.click(screen.getByRole('button', { name: 'Confirmar pagamento' }))

    await waitFor(() => expect(corpo).toEqual({ versao: 1, pagaEm: '2026-03-28' }))
  })

  it('reabrir pede confirmação; cancelar não chama a API', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    let reabriu = false
    server.use(
      ...handlersDeMetasETags(),
      faturasCapturando([{ cartao: BTG, fatura: FECHADA }]).handler,
      http.post(urlDaFatura(FECHADA.id, 'reabrir'), () => {
        reabriu = true
        return HttpResponse.json({ cartao: BTG, fatura: { ...FECHADA, status: 'OPEN', versao: 2 } })
      }),
    )
    const { container } = renderizarRota('/casa/cartoes?mes=2026-03')

    await usuario.click(await screen.findByRole('button', { name: 'Reabrir fatura' }))
    const confirmacao = screen.getByRole('alertdialog', { name: 'Reabrir esta fatura?' })
    expect(within(confirmacao).getByRole('button', { name: 'Cancelar' })).toHaveFocus()
    expect(await violacoesDeAcessibilidade(container)).toEqual([])
    await usuario.click(within(confirmacao).getByRole('button', { name: 'Cancelar' }))
    expect(reabriu).toBe(false)

    await usuario.click(screen.getByRole('button', { name: 'Reabrir fatura' }))
    await usuario.click(
      within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Reabrir fatura' }),
    )
    await waitFor(() => expect(reabriu).toBe(true))
  })

  it('versão desatualizada (409) avisa que a fatura mudou', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    server.use(
      ...handlersDeMetasETags(),
      faturasCapturando([{ cartao: NUBANK, fatura: FATURA_NUBANK }]).handler,
      http.post(urlDaFatura(FATURA_NUBANK.id, 'fechar'), () => problema(409)),
    )
    renderizarRota('/casa/cartoes?mes=2026-03')

    await usuario.click(await screen.findByRole('button', { name: 'Fechar fatura' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('A fatura mudou em outro lugar')
  })

  it('regra recusada (422) mostra o motivo do servidor', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    server.use(
      ...handlersDeMetasETags(),
      faturasCapturando([{ cartao: NUBANK, fatura: FATURA_NUBANK }]).handler,
      http.post(urlDaFatura(FATURA_NUBANK.id, 'fechar'), () =>
        problema(422, 'Esta fatura só pode ser fechada a partir de 25/03/2026'),
      ),
    )
    renderizarRota('/casa/cartoes?mes=2026-03')

    await usuario.click(await screen.findByRole('button', { name: 'Fechar fatura' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('a partir de 25/03/2026')
  })
})

describe('cadastro de cartão', () => {
  it('valida antes de enviar', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    renderizarRota('/casa/cartoes/novo')

    await usuario.click(await screen.findByRole('button', { name: 'Salvar cartão' }))

    expect(await screen.findByText('Informe o nome do cartão')).toBeInTheDocument()
    expect(screen.getByText('Informe o dia de fechamento')).toBeInTheDocument()
    expect(screen.getByText('Informe o dia de vencimento')).toBeInTheDocument()
    expect(screen.getByText('Informe um limite maior que zero')).toBeInTheDocument()
  })

  it('envia o limite como texto da API e volta para Cartões', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    let corpo: unknown
    server.use(
      ...handlersDeMetasETags(),
      http.post(urlDoEspaco(CASA_ID, '/cartoes'), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(NUBANK, { status: 201 })
      }),
      faturasCapturando([{ cartao: NUBANK }]).handler,
    )
    const { router } = renderizarRota('/casa/cartoes/novo')

    await usuario.type(await screen.findByLabelText('Nome'), 'Nubank')
    await usuario.type(screen.getByLabelText('Dia de fechamento'), '25')
    await usuario.type(screen.getByLabelText('Dia de vencimento'), '5')
    await usuario.type(screen.getByLabelText('Limite'), '450000')
    expect(screen.getByLabelText('Limite')).toHaveValue('R$ 4.500,00')
    await usuario.click(screen.getByRole('button', { name: 'Salvar cartão' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/casa/cartoes'))
    expect(corpo).toEqual({ nome: 'Nubank', diaFechamento: 25, diaVencimento: 5, limite: '4500.00' })
  })

  it('nome repetido (422) mostra o motivo', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    server.use(
      http.post(urlDoEspaco(CASA_ID, '/cartoes'), () =>
        problema(422, 'Já existe um cartão com o nome Nubank'),
      ),
    )
    renderizarRota('/casa/cartoes/novo')

    await usuario.type(await screen.findByLabelText('Nome'), 'Nubank')
    await usuario.type(screen.getByLabelText('Dia de fechamento'), '25')
    await usuario.type(screen.getByLabelText('Dia de vencimento'), '5')
    await usuario.type(screen.getByLabelText('Limite'), '100')
    await usuario.click(screen.getByRole('button', { name: 'Salvar cartão' }))

    expect(await screen.findByText('Já existe um cartão com o nome Nubank')).toBeInTheDocument()
  })

  it('editar abre preenchido e o PUT leva a versão', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    let corpo: unknown
    server.use(
      ...handlersDeMetasETags(),
      cartoesNaCasa([NUBANK]),
      http.put(urlDoEspaco(CASA_ID, `/cartoes/${NUBANK.id}`), async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json({ ...NUBANK, nome: 'Nubank Roxo', versao: 1 })
      }),
      faturasCapturando([{ cartao: NUBANK }]).handler,
    )
    renderizarRota(`/casa/cartoes/${NUBANK.id}`)

    const nome = await screen.findByLabelText('Nome')
    expect(nome).toHaveValue('Nubank')
    expect(screen.getByLabelText('Limite')).toHaveValue('R$ 4.500,00')
    await usuario.clear(nome)
    await usuario.type(nome, 'Nubank Roxo')
    await usuario.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    await waitFor(() =>
      expect(corpo).toEqual({
        nome: 'Nubank Roxo',
        diaFechamento: 25,
        diaVencimento: 5,
        limite: '4500.00',
        versao: 0,
      }),
    )
  })

  it('arquivar pede confirmação e mostra a recusa do servidor', async () => {
    const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    server.use(
      cartoesNaCasa([NUBANK]),
      http.delete(urlDoEspaco(CASA_ID, `/cartoes/${NUBANK.id}`), () =>
        problema(422, 'O Nubank tem fatura não paga. Pague as faturas antes de arquivar.'),
      ),
    )
    const { container } = renderizarRota(`/casa/cartoes/${NUBANK.id}`)
    await screen.findByLabelText('Nome')
    expect(await violacoesDeAcessibilidade(container)).toEqual([])

    await usuario.click(screen.getByRole('button', { name: 'Arquivar' }))
    const confirmacao = screen.getByRole('alertdialog', { name: 'Arquivar este cartão?' })
    await usuario.click(within(confirmacao).getByRole('button', { name: 'Arquivar' }))

    expect(await within(confirmacao).findByRole('alert')).toHaveTextContent('fatura não paga')
    expect(await violacoesDeAcessibilidade(container)).toEqual([])
  })

  it('cartão que não existe (arquivado) mostra o caminho de volta', async () => {
    server.use(cartoesNaCasa([]))
    renderizarRota(`/casa/cartoes/${NUBANK.id}`)

    expect(await screen.findByRole('alert')).toHaveTextContent('Cartão não encontrado')
    expect(screen.getByRole('link', { name: 'Voltar para Cartões' })).toBeInTheDocument()
  })
})
