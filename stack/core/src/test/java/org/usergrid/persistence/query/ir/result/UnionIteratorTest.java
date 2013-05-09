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

import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Test;
import org.usergrid.persistence.cassandra.CursorCache;
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
    TreeIterator first = new TreeIterator();
    first.add(id1);
    first.add(id2);
    first.add(id3);
    first.add(id8);
    first.add(id9);
    first.init();
    
    TreeIterator second = new TreeIterator();
    second.add(id1);
    second.add(id2);
    second.add(id3);
    second.add(id4);
    second.add(id8);
    second.add(id10);
    second.init();
    
    TreeIterator third = new TreeIterator();
    third.add(id1);
    third.add(id3);
    third.add(id5);
    third.add(id6);
    third.add(id7);
    third.add(id8);
    third.init();
    

    TreeIterator fourth = new TreeIterator();
    fourth.add(id1);
    fourth.add(id2);
    fourth.add(id3);
    fourth.add(id6);
    fourth.add(id8);
    fourth.add(id10);
    fourth.init();
    
    UnionIterator union = new UnionIterator();
    union.addIterator(first);
    union.addIterator(second);
    union.addIterator(third);
    union.addIterator(fourth);
    
    //now make sure it's right, only 1, 3 and 8 intersect
    assertTrue(union.hasNext());
    assertEquals(id1, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id3, union.next());
    
    assertTrue(union.hasNext());
    assertEquals(id8, union.next());
    
    assertFalse(union.hasNext());
  }
  
  @Test
  public void testOneIterator() {
    
    UUID id1 = UUIDUtils.minTimeUUID(1);
    UUID id2 = UUIDUtils.minTimeUUID(2);
    UUID id3 = UUIDUtils.minTimeUUID(3);
    UUID id4 = UUIDUtils.minTimeUUID(4);
    
    
    //we should get intersection on 1, 3, and 8
    TreeIterator first = new TreeIterator();
    first.add(id1);
    first.add(id2);
    first.add(id3);
    first.add(id4);
    first.init();
    
    
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

  private static class TreeIterator implements ResultIterator {

    private TreeSet<UUID> uuids = new TreeSet<UUID>();
    private Iterator<UUID> iterator;
    

    /**
     * Add a uuid to the list
     * @param ids
     */
    public void add(UUID... ids) {
      for (UUID current : ids) {
        uuids.add(current);
      }
    }

    public void init(){
      this.iterator = uuids.iterator();
    }
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<UUID> iterator() {
      return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    @Override
    public UUID next() {
      return iterator.next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
    }

    /* (non-Javadoc)
     * @see org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(org.usergrid.persistence.cassandra.CursorCache)
     */
    @Override
    public void finalizeCursor(CursorCache cache) {
      
    }

  }

}
