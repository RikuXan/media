package androidx.media3.exoplayer.mediacodec;

final class CodecTimingHistory {
  private final long[] timestampsUs = new long[128];
  private final long[] queuedMs = new long[128];
  private final boolean[] pending = new boolean[128];
  private int next;

  void reset() {
    java.util.Arrays.fill(pending, false);
    next = 0;
  }

  void queued(long timestampUs, long nowMs) {
    timestampsUs[next] = timestampUs;
    queuedMs[next] = nowMs;
    pending[next] = true;
    next = (next + 1) % pending.length;
  }

  long output(long timestampUs, long nowMs) {
    for (int i = 0; i < pending.length; i++) {
      int index = (next + i) % pending.length;
      if (pending[index] && timestampsUs[index] == timestampUs) {
        pending[index] = false;
        return nowMs >= queuedMs[index] ? nowMs - queuedMs[index] : -1;
      }
    }
    return -1;
  }
}
