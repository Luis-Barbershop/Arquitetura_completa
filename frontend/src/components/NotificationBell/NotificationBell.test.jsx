import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import NotificationBell from './NotificationBell'

vi.mock('../../hooks/useNotificationStream', () => ({
  useNotificationStream: vi.fn(),
}))

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('react-toastify', () => ({
  toast: {
    info: vi.fn(),
    success: vi.fn(),
    error: vi.fn(),
  },
}))

import api from '../../services/api'
import { toast } from 'react-toastify'
import { useNotificationStream } from '../../hooks/useNotificationStream'

const notifications = [
  {
    id: 'notification-1',
    title: 'Agendamento confirmado',
    message: 'Seu horario foi confirmado.',
    type: 'APPOINTMENT_CREATED',
    read: false,
    createdAt: '2026-06-12T12:00:00',
  },
  {
    id: 'notification-2',
    title: 'Pagamento aprovado',
    message: 'Pagamento recebido.',
    type: 'PAYMENT_APPROVED',
    read: true,
    createdAt: '2026-06-12T11:00:00',
  },
]

const renderBell = (props = {}) => render(
  <MemoryRouter>
    <NotificationBell userType="customer" {...props} />
  </MemoryRouter>,
)

describe('NotificationBell', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.matchMedia = vi.fn().mockReturnValue({
      matches: false,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })
    vi.mocked(api.get).mockImplementation((url) => {
      if (url === '/notifications/unread-count') {
        return Promise.resolve({ data: { unreadCount: 1 } })
      }
      if (url === '/notifications/my-notifications') {
        return Promise.resolve({ data: notifications })
      }
      return Promise.resolve({ data: {} })
    })
    vi.mocked(api.delete).mockResolvedValue({})
    vi.mocked(useNotificationStream).mockImplementation(() => {})
  })

  it('limpa todas as notificações pelo botão fixo do dropdown', async () => {
    renderBell()

    fireEvent.click(screen.getByRole('button', { name: 'Notificações' }))

    expect(await screen.findByText('Agendamento confirmado')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Limpar todas' }))

    await waitFor(() => {
      expect(api.delete).toHaveBeenCalledWith('/notifications/my-notifications')
    })

    expect(await screen.findByText('Nenhuma notificação.')).toBeInTheDocument()
    expect(screen.queryByText('Agendamento confirmado')).not.toBeInTheDocument()
    expect(screen.queryByText('Limpar todas')).not.toBeInTheDocument()
    expect(screen.queryByText('1')).not.toBeInTheDocument()
    expect(toast.success).toHaveBeenCalledWith('Notificações limpas.')
  })

  it('mantém a lista e mostra erro quando a limpeza falha', async () => {
    vi.mocked(api.delete).mockRejectedValueOnce(new Error('network'))
    renderBell()

    fireEvent.click(screen.getByRole('button', { name: 'Notificações' }))
    expect(await screen.findByText('Agendamento confirmado')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Limpar todas' }))

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Não foi possível limpar as notificações agora.')
    })
    expect(screen.getByText('Agendamento confirmado')).toBeInTheDocument()
  })

  it('mostra notificações do barbeiro na versão mobile', async () => {
    window.matchMedia = vi.fn().mockReturnValue({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })

    renderBell({ userType: 'barber', visibility: 'mobile' })

    expect(await screen.findByText('1')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Notificações' }))

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/notifications/my-notifications')
    })
    expect(await screen.findByText('Agendamento confirmado')).toBeInTheDocument()
    expect(screen.getByText('Pagamento aprovado')).toBeInTheDocument()
  })
})
