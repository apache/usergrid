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
package org.usergrid.persistence.cassandra.index;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NavigableSet;

import me.prettyprint.hector.api.beans.HColumn;

/**
 * Index scanner that doesn't return anything.  This is used if our cursor has advanced beyond the end of all scannable ranges
 * @author tnine
 *
 */
public class NoOpIndexScanner implements IndexScanner{

  /**
   * 
   */
  public NoOpIndexScanner() {
  }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<NavigableSet<HColumn<ByteBuffer, ByteBuffer>>> iterator() {
    return this;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    return false;
  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.cassandra.index.IndexScanner#reset()
   */
  @Override
  public void reset() {
    //no op
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  @Override
  public NavigableSet<HColumn<ByteBuffer, ByteBuffer>> next() {
    return null;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported");
  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.cassandra.index.IndexScanner#getPageSize()
   */
  @Override
  public int getPageSize() {
    return 0;
  }

}
