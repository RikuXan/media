package androidx.media3.extractor.mp4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.test.utils.FakeExtractorInput;
import androidx.media3.test.utils.FakeExtractorOutput;
import androidx.media3.test.utils.FakeTrackOutput;
import androidx.media3.test.utils.TestUtil;
import androidx.test.core.app.ApplicationProvider;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FragmentedMp4EmsgTimestampTest {

  @Test
  public void relativeEmsgAppliesTimestampOffsetOnce() throws Exception {
    TimestampAdjuster adjuster = new TimestampAdjuster(5000000);
    assertEquals(5000000, extractMetadataTime(0, 0, adjuster));
  }

  @Test
  public void relativeEmsgRetainsNonzeroPresentationDelta() throws Exception {
    TimestampAdjuster adjuster = new TimestampAdjuster(5000000);
    assertEquals(5034000, extractMetadataTime(0, 34000, adjuster));
  }

  @Test
  public void pendingAbsoluteEmsgStillAppliesOffsetOnce() throws Exception {
    TimestampAdjuster adjuster = new TimestampAdjuster(5000000);
    long metadataTime = extractMetadataTime(1, 1000000, adjuster);
    assertEquals(1000000 + adjuster.getTimestampOffsetUs(), metadataTime);
  }

  private static long extractMetadataTime(int version, long timestamp, TimestampAdjuster adjuster) throws Exception {
    byte[] media = TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), "media/mp4/sample_fragmented.mp4");
    ByteArrayOutputStream data = new ByteArrayOutputStream();
    data.write(emsg(version, timestamp));
    data.write(media);
    FakeExtractorInput input = new FakeExtractorInput.Builder().setData(data.toByteArray()).build();
    FakeExtractorOutput output = new FakeExtractorOutput();
    FragmentedMp4Extractor extractor = new FragmentedMp4Extractor(FragmentedMp4Extractor.FLAG_ENABLE_EMSG_TRACK, adjuster);
    extractor.init(output);
    PositionHolder seek = new PositionHolder();
    int result;
    while ((result = extractor.read(input, seek)) != Extractor.RESULT_END_OF_INPUT) {
      if (result == Extractor.RESULT_SEEK) input.setPosition((int) seek.position);
    }
    extractor.release();
    for (int i = 0; i < output.trackOutputs.size(); i++) {
      FakeTrackOutput track = output.trackOutputs.valueAt(i);
      if (track.lastFormat != null && MimeTypes.APPLICATION_EMSG.equals(track.lastFormat.sampleMimeType)) {
        assertEquals(1, track.getSampleCount());
        return track.getSampleTimeUs(0);
      }
    }
    fail("Missing EMSG output");
    return 0;
  }

  private static byte[] emsg(int version, long timestamp) throws Exception {
    ByteArrayOutputStream payload = new ByteArrayOutputStream();
    DataOutputStream writer = new DataOutputStream(payload);
    writer.writeInt(version << 24);
    if (version == 0) writer.writeBytes("urn:twitch:id3\0\0");
    writer.writeInt(1000000);
    if (version == 0) writer.writeInt((int) timestamp); else writer.writeLong(timestamp);
    writer.writeInt(1000);
    writer.writeInt(1);
    if (version == 1) writer.writeBytes("urn:twitch:id3\0\0");
    writer.write(new byte[] {'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0});
    ByteArrayOutputStream atom = new ByteArrayOutputStream();
    DataOutputStream header = new DataOutputStream(atom);
    header.writeInt(payload.size() + 8);
    header.writeBytes("emsg");
    payload.writeTo(atom);
    return atom.toByteArray();
  }
}
