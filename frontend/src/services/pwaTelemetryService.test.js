import { describe, expect, it, vi } from 'vitest';

import { PWA_METRICS, trackPwaMetric } from './pwaTelemetryService';

describe('pwaTelemetryService', () => {
  it('emits browser and data layer telemetry events', () => {
    const listener = vi.fn();
    const consoleSpy = vi.spyOn(console, 'info').mockImplementation(() => {});

    window.dataLayer = [];
    window.addEventListener('cortaai:pwa-metric', listener);

    trackPwaMetric(PWA_METRICS.SW_REGISTER_SUCCESS, { source: 'test' });

    expect(listener).toHaveBeenCalledTimes(1);
    expect(listener.mock.calls[0][0].detail.metric).toBe(PWA_METRICS.SW_REGISTER_SUCCESS);
    expect(listener.mock.calls[0][0].detail.metadata).toEqual({ source: 'test' });
    expect(window.dataLayer).toHaveLength(1);
    expect(window.dataLayer[0]).toEqual(
      expect.objectContaining({
        event: 'pwa_metric',
        metric: PWA_METRICS.SW_REGISTER_SUCCESS,
        metadata: { source: 'test' },
      }),
    );

    window.removeEventListener('cortaai:pwa-metric', listener);
    consoleSpy.mockRestore();
  });
});