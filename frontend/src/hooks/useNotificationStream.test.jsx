import { renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.unmock('./useNotificationStream');

import { useNotificationStream } from './useNotificationStream';

class FakeEventSource {
  static instances = [];

  constructor(url) {
    this.url = url;
    this.listeners = {};
    this.close = vi.fn();
    FakeEventSource.instances.push(this);
  }

  addEventListener(type, listener) {
    this.listeners[type] = listener;
  }

  emit(type, data) {
    this.listeners[type]?.({ data });
  }
}

describe('useNotificationStream', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    FakeEventSource.instances = [];
    global.EventSource = FakeEventSource;
  });

  afterEach(() => {
    vi.useRealTimers();
    delete global.EventSource;
  });

  it('connects with the stored token and emits unread counts', () => {
    localStorage.setItem('token', 'tok en');
    const onUnreadCount = vi.fn();

    const { unmount } = renderHook(() => useNotificationStream(onUnreadCount));

    expect(FakeEventSource.instances[0].url).toContain('token=tok%20en');
    FakeEventSource.instances[0].emit('unread-count', JSON.stringify({ unreadCount: 7 }));
    FakeEventSource.instances[0].emit('unread-count', '{broken');

    expect(onUnreadCount).toHaveBeenCalledWith(7);
    unmount();
    expect(FakeEventSource.instances[0].close).toHaveBeenCalled();
  });

  it('does not connect without a token', () => {
    renderHook(() => useNotificationStream(vi.fn()));

    expect(FakeEventSource.instances).toHaveLength(0);
  });
});
