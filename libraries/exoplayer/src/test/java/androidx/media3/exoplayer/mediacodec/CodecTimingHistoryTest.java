package androidx.media3.exoplayer.mediacodec;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CodecTimingHistoryTest {
  @Test public void matchesReorderedOutputByTimestamp() {
    CodecTimingHistory h = new CodecTimingHistory();
    h.queued(100, 10);
    h.queued(50, 20);
    assertEquals(5, h.output(50, 25));
    assertEquals(20, h.output(100, 30));
    assertEquals(-1, h.output(100, 31));
  }
  @Test public void repeatedTimestampsUseOldestPendingInput() {
    CodecTimingHistory h = new CodecTimingHistory();
    h.queued(100, 10);
    h.queued(100, 20);
    assertEquals(20, h.output(100, 30));
    assertEquals(11, h.output(100, 31));
  }
  @Test public void resetAndEvictionDoNotInventDurations() {
    CodecTimingHistory h = new CodecTimingHistory();
    for (int i = 0; i < 129; i++) h.queued(i, i);
    assertEquals(-1, h.output(0, 200));
    assertEquals(72, h.output(128, 200));
    h.reset();
    assertEquals(-1, h.output(127, 200));
  }
  @Test public void invalidClockDeltaIsUnavailable() {
    CodecTimingHistory h = new CodecTimingHistory();
    h.queued(-100, 20);
    assertEquals(-1, h.output(-100, 10));
  }
}
