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
package org.usergrid.persistence.query.ir.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;
import org.usergrid.utils.UUIDUtils;

/**
 * @author tnine
 * 
 */
public class UnionIteratorTest {

  @Test
  public void testMutipleIterators() {
    
    UUID id1 = UUIDUtils.minTimeUUID(1);
    UUID id2 = UUIDUtils.minTimeUUID(2);
    UUID id3 = UUIDUtils.minTimeUUID(3);
    UUID id4 = UUIDUtils.minTimeUUID(4);
    UUID id5 = UUIDUtils.minTimeUUID(5);
    UUID id6 = UUIDUtils.minTimeUUID(6);
    UUID id7 = UUIDUtils.minTimeUUID(7);
    UUID id8 = UUIDUtils.minTimeUUID(8);
    UUID id9 = UUIDUtils.minTimeUUID(9);
    UUID id10 = UUIDUtils.minTimeUUID(10);
    
    
    //we should get intersection on 1, 3, and 8
    InOrderIterator first = new InOrderIterator(100);
    first.add(id1);
    first.add(id2);
    first.add(id3);
    first.add(id8);
    first.add(id9);
 
    
    InOrderIterator second = new InOrderIterator(100);
    second.add(id1);
    second.add(id2);
    second.add(id3);
    second.add(id4);
    second.add(id8);
    second.add(id10);
    
    InOrderIterator third = new InOrderIterator(100);
    third.add(id1);
    third.add(id3);
    third.add(id5);
    third.add(id6);
    third.add(id7);
    third.add(id8);
    

    InOrderIterator fourth = new InOrderIterator(100);
    fourth.add(id1);
    fourth.add(id2);
    fourth.add(id3);
    fourth.add(id6);
    fourth.add(id8);
    fourth.add(id9);
    
    UnionIterator union = new UnionIterator();
    union.addIterator(first);
    union.addIterator(second);
    union.addIterator(third);
    union.addIterator(fourth);
    
    //now make sure it's right, only 1, 3 and 8 intersect
    assertTrue(union.hasNext());
    assertEquals(id1, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id2, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id3, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id4, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id5, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id6, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id7, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id8, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id9, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id10, union.next());
    
    assertFalse(union.hasNext());
  }
  
  @Test
  public void testOneIterator() {
    
    UUID id1 = UUIDUtils.minTimeUUID(1);
    UUID id2 = UUIDUtils.minTimeUUID(2);
    UUID id3 = UUIDUtils.minTimeUUID(3);
    UUID id4 = UUIDUtils.minTimeUUID(4);
    
    
    //we should get intersection on 1, 3, and 8
    InOrderIterator first = new InOrderIterator(100);
    first.add(id1);
    first.add(id2);
    first.add(id3);
    first.add(id4);
    
    
    UnionIterator union = new UnionIterator();
    union.addIterator(first);
    
    //now make sure it's right, only 1, 3 and 8 intersect
    assertTrue(union.hasNext());
    assertEquals(id1, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id2, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id3, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id4, union.next());
    
    assertFalse(union.hasNext());
  }
  
  @Test
  public void testNoIterator() {
    
    
    
    UnionIterator union = new UnionIterator();
    
    
    //now make sure it's right, only 1, 3 and 8 intersect
    assertFalse(union.hasNext());
  }


  
}
