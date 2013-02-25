/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.utils;

import static com.fasterxml.uuid.impl.UUIDUtil.BYTE_OFFSET_CLOCK_HI;
import static com.fasterxml.uuid.impl.UUIDUtil.BYTE_OFFSET_CLOCK_LO;
import static com.fasterxml.uuid.impl.UUIDUtil.BYTE_OFFSET_CLOCK_MID;
import static com.fasterxml.uuid.impl.UUIDUtil.BYTE_OFFSET_CLOCK_SEQUENCE;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.usergrid.utils.ConversionUtils.bytes;
import static org.usergrid.utils.ConversionUtils.uuid;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.UUIDComparator;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

/**
 * @author edanuff
 * 
 */
public class UUIDUtils {

  public static final UUID MIN_TIME_UUID = UUID.fromString("00000000-0000-1000-8000-000000000000");

  public static final UUID MAX_TIME_UUID = UUID.fromString("ffffffff-ffff-1fff-bfff-ffffffffffff");

  public static final TimeBasedGenerator generator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
  /**
	 * 
	 */
  public static final UUID zeroUUID = new UUID(0, 0);

  /**
   * @return
   */
  public static java.util.UUID newTimeUUID() {
    return generator.generate();
  }

  private final static long kClockOffset = 0x01b21dd213814000L;
  private final static long kClockMultiplierL = 10000L;

  private final static Random clockSequenceRandom = new Random();
  private final static Random timeResolutionRandom = new Random();

  // 13 bits of randomness
  private static int getRandomTimeResolution() {
    return timeResolutionRandom.nextInt() & 0x1FFF;
  }

  // 14 bits of randomness
  private static int getRandomClockSequence() {
    return clockSequenceRandom.nextInt() & 0x3FFF;
  }

  private static void setTimestamp(long timestamp, byte[] uuidBytes, int clockSeq, int timeOffset) {

    timestamp *= kClockMultiplierL;
    timestamp += kClockOffset;
    timestamp += timeOffset;

    // Set random clock sequence
    uuidBytes[BYTE_OFFSET_CLOCK_SEQUENCE] = (byte) (clockSeq >> 8);
    uuidBytes[BYTE_OFFSET_CLOCK_SEQUENCE + 1] = (byte) clockSeq;

    // Set variant
    uuidBytes[BYTE_OFFSET_CLOCK_SEQUENCE] &= 0x3F;
    uuidBytes[BYTE_OFFSET_CLOCK_SEQUENCE] |= 0x80;

    // Time fields aren't nicely split across the UUID, so can't just
    // linearly dump the stamp:
    int clockHi = (int) (timestamp >>> 32);
    int clockLo = (int) timestamp;

    uuidBytes[BYTE_OFFSET_CLOCK_HI] = (byte) (clockHi >>> 24);
    uuidBytes[BYTE_OFFSET_CLOCK_HI + 1] = (byte) (clockHi >>> 16);
    uuidBytes[BYTE_OFFSET_CLOCK_MID] = (byte) (clockHi >>> 8);
    uuidBytes[BYTE_OFFSET_CLOCK_MID + 1] = (byte) clockHi;

    uuidBytes[BYTE_OFFSET_CLOCK_LO] = (byte) (clockLo >>> 24);
    uuidBytes[BYTE_OFFSET_CLOCK_LO + 1] = (byte) (clockLo >>> 16);
    uuidBytes[BYTE_OFFSET_CLOCK_LO + 2] = (byte) (clockLo >>> 8);
    uuidBytes[BYTE_OFFSET_CLOCK_LO + 3] = (byte) clockLo;

    // Set version
    uuidBytes[BYTE_OFFSET_CLOCK_HI] &= 0x0F;
    uuidBytes[BYTE_OFFSET_CLOCK_HI] |= 0x10;

  }

  /**
   * Generate a timeuuid with the given timestamp in milliseconds and the time
   * offset. Useful when you need to generate sequential UUIDs for the same
   * period in time. I.E
   * 
   * newTimeUUID(1000, 0) <br/>
   * newTimeUUID(1000, 1) <br />
   * newTimeUUID(1000, 2) <br />
   * 
   * etc.
   * 
   * Only use this method if you are absolutely sure you need it. When it doubt
   * use the method without the timestamp offset
   * 
   * @param ts
   *          The timestamp in milliseconds
   * @param timeoffset
   *          The offset, which should always be <= 10000. If you go beyond this
   *          range, the millisecond will be incremented since this is beyond
   *          the possible values when coverrting from millis to 1/10 microseconds stored in the time uuid.
   * @return
   */
  public static UUID newTimeUUID(long ts, int timeoffset) {
    if (ts == 0) {
      return newTimeUUID();
    }

    byte[] uuidBytes = new byte[16];
    // 47 bits of randomness
    EthernetAddress eth = EthernetAddress.constructMulticastAddress();
    eth.toByteArray(uuidBytes, 10);
    setTimestamp(ts, uuidBytes, getRandomClockSequence(), timeoffset);

    return uuid(uuidBytes);
  }

  /**
   * Generate a new UUID with the given time stamp in milliseconds
   * 
   * @param ts
   * @return
   */
  public static UUID newTimeUUID(long ts) {
    return newTimeUUID(ts, getRandomTimeResolution());
  }

  public static UUID minTimeUUID(long ts) {
    byte[] uuidBytes = new byte[16];
    setTimestamp(ts, uuidBytes, 0, 0);

    return uuid(uuidBytes);
  }

  public static UUID maxTimeUUID(long ts) {
    byte[] uuidBytes = new byte[16];
    uuidBytes[10] = (byte) 0xFF;
    uuidBytes[11] = (byte) 0xFF;
    uuidBytes[12] = (byte) 0xFF;
    uuidBytes[13] = (byte) 0xFF;
    uuidBytes[14] = (byte) 0xFF;
    uuidBytes[15] = (byte) 0xFF;
    setTimestamp(ts, uuidBytes, 0x3FFF, 0x1FFF);

    return uuid(uuidBytes);
  }

  /**
   * @param uuid
   * @return
   */
  public static boolean isTimeBased(UUID uuid) {
    if (uuid == null) {
      return false;
    }
    return uuid.version() == 1;
  }

  public static long getTimestampInMillis(UUID uuid) {
    if (uuid == null) {
      return 0;
    }
    long t = uuid.timestamp();
    long timeMillis = (t - kClockOffset) / kClockMultiplierL;
    return timeMillis;
  }

  public static long getTimestampInMicros(UUID uuid) {
    if (uuid == null) {
      return 0;
    }
    long t = uuid.timestamp();
    long timeMillis = (t - kClockOffset) / 10;
    return timeMillis;
  }

  public static UUID tryGetUUID(String s) {
    if (s == null) {
      return null;
    }
    if (s.length() != 36) {
      return null;
    }
    // 8-4-4-4-12
    // 0-7,8,9-12,13,14-17,18,19-22,23,24-35
    if (s.charAt(8) != '-') {
      return null;
    }
    if (s.charAt(13) != '-') {
      return null;
    }
    if (s.charAt(18) != '-') {
      return null;
    }
    if (s.charAt(23) != '-') {
      return null;
    }
    UUID uuid = null;
    try {
      uuid = UUID.fromString(s);
    } catch (Exception e) {
    }
    return uuid;
  }

  public static boolean isUUID(String s) {
    return tryGetUUID(s) != null;
  }

  public static boolean startsWithUUID(String s) {
    if (s == null) {
      return false;
    }
    if (s.length() < 36) {
      return false;
    }
    return isUUID(s.substring(0, 36));
  }

  public static UUID tryExtractUUID(String s) {
    if (s == null) {
      return null;
    }
    if (s.length() < 36) {
      return null;
    }
    return tryGetUUID(s.substring(0, 36));
  }

  public static UUID tryExtractUUID(String s, int offset) {
    if (s == null) {
      return null;
    }
    if ((s.length() - offset) < 36) {
      return null;
    }
    return tryGetUUID(s.substring(offset, offset + 36));
  }

  public static String toBase64(UUID id) {
    if (id == null) {
      return null;
    }
    return encodeBase64URLSafeString(bytes(id));
  }

  public static UUID fromBase64(String str) {
    if (str == null) {
      return null;
    }
    byte[] bytes = decodeBase64(str);
    if (bytes.length != 16) {
      return null;
    }
    return uuid(bytes);
  }

  public static int compare(UUID u1, UUID u2) {
    return UUIDComparator.staticCompare(u1, u2);
  }

  public static List<UUID> sort(List<UUID> uuids) {
    Collections.sort(uuids, new UUIDComparator());
    return uuids;
  }

  public static List<UUID> sortReversed(List<UUID> uuids) {
    Collections.sort(uuids, new Comparator<UUID>() {
      @Override
      public int compare(UUID u1, UUID u2) {
        return UUIDComparator.staticCompare(u2, u1);
      }
    });
    return uuids;
  }

}
