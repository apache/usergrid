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

import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_COMPOSITE_DICTIONARIES;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.hector.api.beans.HColumn;

import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.ConnectionRefImpl;

import com.yammer.metrics.annotation.Metered;

/**
 * @author tnine
 * 
 */
public class ConnectedIndexScanner implements IndexScanner {

  private final CassandraService cass;
  private final UUID applicationId;
  private final boolean reversed;
  private final int pageSize;
  private final String dictionaryType;
  private final ConnectionRefImpl connection;
  /**
   * Pointer to our next start read
   */
  private ByteBuffer start;

  /**
   * Set to the original value to start scanning from
   */
  private ByteBuffer scanStart;

  /**
   * Iterator for our results from the last page load
   */
  private LinkedHashSet<HColumn<ByteBuffer, ByteBuffer>> lastResults;

  /**
   * True if our last load loaded a full page size.
   */
  private boolean hasMore = true;

  public ConnectedIndexScanner(CassandraService cass, String dictionaryType, UUID applicationId,
      ConnectionRefImpl connection, ByteBuffer start, boolean reversed, int pageSize) {

    // create our start and end ranges
    this.scanStart = start;
    this.cass = cass;
    this.applicationId = applicationId;
    this.start = scanStart;
    this.reversed = reversed;
    this.pageSize = pageSize;
    this.connection = connection;
    this.dictionaryType = dictionaryType;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.cassandra.index.IndexScanner#reset()
   */
  @Override
  public void reset() {
    hasMore = true;
    start = scanStart;
  }

  /**
   * Search the collection index using all the buckets for the given collection.
   * Load the next page. Return false if nothing was loaded, true otherwise
   * 
   * @param indexKey
   * @param slice
   * @param count
   * @param collectionName
   * @return
   * @throws Exception
   */

  public boolean load() throws Exception {

    // nothing left to load
    if (!hasMore) {
      return false;
    }

    // if we skip the first we need to set the load to page size +2, since we'll
    // discard the first
    // and start paging at the next entity, otherwise we'll just load the page
    // size we need
    int selectSize = pageSize + 1;
    //
    // addInsertToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
    // key(connection.getConnectingEntityId(), DICTIONARY_CONNECTED_ENTITIES,
    // connection.getConnectionType(), connection.getConnectedEntityType()),
    //
    Object key = null;

    key = key(connection.getConnectingEntityId(), dictionaryType, connection.getConnectionType());
    

    List<HColumn<ByteBuffer, ByteBuffer>> results = cass.getColumns(cass.getApplicationKeyspace(applicationId),
        ENTITY_COMPOSITE_DICTIONARIES, key, start, null, selectSize, reversed);

    // we loaded a full page, there might be more
    if (results.size() == selectSize) {
      hasMore = true;

      // set the bytebuffer for the next pass
      start = results.get(results.size() - 1).getName();

      results.remove(results.size() - 1);

    } else {
      hasMore = false;
    }

    lastResults = new LinkedHashSet<HColumn<ByteBuffer, ByteBuffer>>(results);

    return lastResults != null && lastResults.size() > 0;

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Set<HColumn<ByteBuffer, ByteBuffer>>> iterator() {
    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {

    // We've either 1) paged everything we should and have 1 left from our
    // "next page" pointer
    // Our currently buffered results don't exist or don't have a next. Try to
    // load them again if they're less than the page size
    if (lastResults == null && hasMore) {
      try {
        return load();
      } catch (Exception e) {
        throw new RuntimeException("Error loading next page of indexbucket scanner", e);
      }
    }

    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  @Metered(group = "core", name = "IndexBucketScanner_load")
  public Set<HColumn<ByteBuffer, ByteBuffer>> next() {
    Set<HColumn<ByteBuffer, ByteBuffer>> returnVal = lastResults;

    lastResults = null;

    return returnVal;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("You can't remove from a result set, only advance");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.cassandra.index.IndexScanner#getPageSize()
   */
  @Override
  public int getPageSize() {
    return pageSize;
  }
}
