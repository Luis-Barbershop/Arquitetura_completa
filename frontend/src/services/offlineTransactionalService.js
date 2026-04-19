const OFFLINE_TRANSACIONAL_CODE = 'OFFLINE_TRANSACIONAL_UNAVAILABLE'

const DEFAULT_OFFLINE_TRANSACIONAL_MESSAGE =
  'Você está offline e esta operação transacional exige conexão. Tente novamente quando a internet voltar.'

export const isOfflineTransactionalError = (error) => {
  const status = error?.response?.status
  const code = error?.response?.data?.error

  return status === 503 && code === OFFLINE_TRANSACIONAL_CODE
}

export const getOfflineTransactionalMessage = (error) => {
  const apiMessage = error?.response?.data?.message
  if (apiMessage && typeof apiMessage === 'string') {
    return apiMessage
  }

  return DEFAULT_OFFLINE_TRANSACIONAL_MESSAGE
}
