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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.UUIDUtils.getTimestampInMillis;
import static org.usergrid.utils.UUIDUtils.newTimeUUID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UUIDUtilsTest {

  private static final Logger logger = LoggerFactory.getLogger(UUIDUtilsTest.class);

  @Test
  public void testUUIDUtils() {
    UUID uuid = UUIDUtils.newTimeUUID();
    logger.info("" + uuid);
    logger.info("" + uuid.timestamp());
    logger.info("" + UUIDUtils.getTimestampInMillis(uuid));

    logger.info("" + UUIDUtils.getTimestampInMillis(UUIDUtils.newTimeUUID()));
    logger.info("" + System.currentTimeMillis());

    logger.info("" + UUIDUtils.getTimestampInMicros(UUIDUtils.newTimeUUID()));
    logger.info("" + (System.currentTimeMillis() * 1000));

    logger.info("" + UUIDUtils.MIN_TIME_UUID);
    logger.info("" + UUIDUtils.MIN_TIME_UUID.variant());
    logger.info("" + UUIDUtils.MIN_TIME_UUID.version());
    logger.info("" + UUIDUtils.MIN_TIME_UUID.clockSequence());
    logger.info("" + UUIDUtils.MIN_TIME_UUID.timestamp());

    logger.info("" + UUIDUtils.MAX_TIME_UUID);
    logger.info("" + UUIDUtils.MAX_TIME_UUID.variant());
    logger.info("" + UUIDUtils.MAX_TIME_UUID.version());
    logger.info("" + UUIDUtils.MAX_TIME_UUID.clockSequence());
    logger.info("" + UUIDUtils.MAX_TIME_UUID.timestamp());
  }

  @Test
  public void testAppProvidedTimestamp() {
    logger.info("UUIDUtilsTest.testAppProvidedTimestamp");
    long ts = System.currentTimeMillis();
    System.out.println(ts);

    Set<UUID> uuids = new HashSet<UUID>();

    int count = 1000000;

    logger.info("Generating " + count + " UUIDs...");
    for (int i = 0; i < count; i++) {
      UUID uuid = newTimeUUID(ts);

      assertFalse("UUID already generated", uuids.contains(uuid));
      uuids.add(uuid);

      assertEquals("Incorrect UUID timestamp value", ts, getTimestampInMillis(uuid));
    }
    logger.info("UUIDs checked");

  }

  @Test
  public void testAppProvidedTimestampOrdering() {
    logger.info("UUIDUtilsTest.testAppProvidedTimestamp");
    long ts = System.currentTimeMillis();
    System.out.println(ts);

    UUID first = newTimeUUID(ts, 0);

    UUID second = newTimeUUID(ts, 1);

    assertFalse(first.equals(second));
    assertTrue(first.compareTo(second) < 0);

  }

  @Test
  public void timeUUIDOrdering() {
    int count = 10000;

    long ts = System.currentTimeMillis();

    List<UUID> uuids = new ArrayList<UUID>(count);

    logger.info("Generating " + count + " UUIDs...");
    for (int i = 0; i < count; i++) {
      UUID uuid = newTimeUUID(ts, i);

      uuids.add(uuid);

      assertEquals("Incorrect UUID timestamp value", ts, getTimestampInMillis(uuid));
    }

    for (int i = 0; i < count - 1; i++) {
      assertEquals(-1, uuids.get(i).compareTo(uuids.get(i + 1)));
    }
  }

  @Test
  public void directUuidFrob() {
    long startTime = System.currentTimeMillis();
    int count = 1000* 1000;
    HashSet created = new HashSet(count);
    long tsmicros;
    for (int x=0; x< count; x++) {
      tsmicros = UUIDUtils.getTimestampInMicros(UUIDUtils.newTimeUUID());
      created.add(tsmicros);
    }
    logger.info("execution took {}", System.currentTimeMillis() - startTime);
    assertEquals(count, created.size());
    assertTrue(created.size() > 0);
  }

  @Test
  public void concurrentUuidFrob() throws Exception {
    long startTime = System.currentTimeMillis();
    List<Future> jobs = executeFrob();
    for ( Future f: jobs ) {
      logger.info("waiting on job...");
      f.get();
    }
    logger.info("execution took {}", System.currentTimeMillis() - startTime);
  }

  private List<Future> executeFrob() {
    ExecutorService exec = Executors.newFixedThreadPool(5);
    List<Future> jobs  = new ArrayList<Future>(10);
    for (int x=0; x<10; x++){
      jobs.add(exec.submit(new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          logger.info("call invoked");
          int count = 1000* 100;
              HashSet created = new HashSet(count);
              long tsmicros;
              for (int x=0; x< count; x++) {
                tsmicros = UUIDUtils.getTimestampInMicros(UUIDUtils.newTimeUUID());
                created.add(tsmicros);
              }

              assertEquals(count, created.size());
              assertTrue(created.size() > 0);
          logger.info("run complete");
          return null;
        }
      }));
    }
    return jobs;
  }

  @Test
  public void timeUUIDOrderingRolls() {

    long ts = System.currentTimeMillis();

    UUID first = newTimeUUID(ts, 0);

    assertEquals(ts, getTimestampInMillis(first));
    
    UUID second = newTimeUUID(ts, 10001);
    
    assertEquals(ts+1, getTimestampInMillis(second));
    
    
  }
  
  
  @Test
  public void timeUUIDOrderingGaps() throws InterruptedException {

    UUID now1 = newTimeUUID();
    UUID now2 = newTimeUUID();
    
    Thread.sleep(1);
    
    long start  = System.currentTimeMillis();
    
    UUID t1 = newTimeUUID(start, 0);
    UUID t2 = newTimeUUID(start, 1);
    
    UUID now3 = newTimeUUID();
    

    assertEquals(-1, now1.compareTo(t1));
    assertEquals(-1, now2.compareTo(t1));
    
    assertEquals(-1, now1.compareTo(t2));
    assertEquals(-1, now2.compareTo(t2));
    
    assertEquals(-1, t1.compareTo(now3));
    assertEquals(-1, t2.compareTo(now3));
    
    
    
  }
  
  @Test
  public void max(){
    long start = System.currentTimeMillis();
    
    UUID t1 = newTimeUUID(start, 0);
    UUID t2 = newTimeUUID(start, 1);
    
    assertEquals(t2, UUIDUtils.max(t1, t2));
  }
  
  @Test
  public void maxFirstNull(){
    long start = System.currentTimeMillis();
    
    UUID t1 = newTimeUUID(start, 0);
    
    assertEquals(t1, UUIDUtils.max(null, t1));
  }
  
  @Test
  public void maxSecondNull(){
    long start = System.currentTimeMillis();
    
    UUID t1 = newTimeUUID(start, 0);
    
    assertEquals(t1, UUIDUtils.max(t1, null));
  }
  
  @Test
  public void maxBothNull(){
    assertNull(UUIDUtils.max(null, null));
  }

  
  @Test
  public void min(){
    long start = System.currentTimeMillis();
    
    UUID t1 = newTimeUUID(start, 0);
    UUID t2 = newTimeUUID(start, 1);
    
    assertEquals(t1, UUIDUtils.min(t1, t2));
  }
  
  @Test
  public void minFirstNull(){
    long start = System.currentTimeMillis();
    
    UUID t1 = newTimeUUID(start, 0);
    
    assertEquals(t1, UUIDUtils.min(null, t1));
  }
  
  @Test
  public void minSecondNull(){
    long start = System.currentTimeMillis();
    
    UUID t1 = newTimeUUID(start, 0);
    
    assertEquals(t1, UUIDUtils.min(t1, null));
  }
  
  @Test
  public void minBothNull(){
    assertNull(UUIDUtils.min(null, null));
  }

}
