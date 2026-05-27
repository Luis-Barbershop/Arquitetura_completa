import { describe, expect, it } from 'vitest';

import { getOfflineTransactionalMessage, isOfflineTransactionalError } from './offlineTransactionalService';

describe('offlineTransactionalService', () => {
  it('detects the offline transactional error signature', () => {
    expect(
      isOfflineTransactionalError({
        response: { status: 503, data: { error: 'OFFLINE_TRANSACIONAL_UNAVAILABLE' } },
      }),
    ).toBe(true);

    expect(
      isOfflineTransactionalError({
        response: { status: 500, data: { error: 'OFFLINE_TRANSACIONAL_UNAVAILABLE' } },
      }),
    ).toBe(false);
  });

  it('returns backend message or the default fallback', () => {
    expect(
      getOfflineTransactionalMessage({
        response: { data: { message: 'Mensagem do backend' } },
      }),
    ).toBe('Mensagem do backend');

    expect(getOfflineTransactionalMessage({})).toBe(
      'Você está offline e esta operação transacional exige conexão. Tente novamente quando a internet voltar.',
    );
  });
});