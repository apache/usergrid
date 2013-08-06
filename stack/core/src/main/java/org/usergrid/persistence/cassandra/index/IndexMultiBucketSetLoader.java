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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.thrift.CassandraServer;
import org.usergrid.persistence.cassandra.ApplicationCF;
import org.usergrid.persistence.cassandra.CassandraService;

import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;

/**
 * @author tnine
 * 
 */
public class IndexMultiBucketSetLoader {


  
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  
  /**
   * Loads and sorts columns from each bucket in memory.  This will return a contiguous set of columns as if they'd been read from a single row
   * @param cass
   * @param columnFamily
   * @param applicationId
   * @param rowKeys
   * @param start
   * @param finish
   * @param resultSize
   * @param reversed
   * @return
   * @throws Exception
   */
  public static TreeSet<HColumn<ByteBuffer, ByteBuffer>> load(CassandraService cass, ApplicationCF columnFamily, UUID applicationId, List<Object> rowKeys, Object start, Object finish, int resultSize, boolean reversed) throws Exception {
    Map<ByteBuffer, List<HColumn<ByteBuffer, ByteBuffer>>> results = cass.multiGetColumns(
        cass.getApplicationKeyspace(applicationId), columnFamily, rowKeys, start, finish, resultSize, reversed);

    final Comparator<ByteBuffer> comparator = reversed ? new DynamicCompositeReverseComparator(columnFamily)
        : new DynamicCompositeForwardComparator(columnFamily);

    TreeSet<HColumn<ByteBuffer, ByteBuffer>> resultsTree = new TreeSet<HColumn<ByteBuffer, ByteBuffer>>(
        new Comparator<HColumn<ByteBuffer, ByteBuffer>>() {

          @Override
          public int compare(HColumn<ByteBuffer, ByteBuffer> first, HColumn<ByteBuffer, ByteBuffer> second) {

            return comparator.compare(first.getName(), second.getName());
          }

        });

    for (List<HColumn<ByteBuffer, ByteBuffer>> cols : results.values()) {

      for (HColumn<ByteBuffer, ByteBuffer> col : cols) {
        resultsTree.add(col);

        // trim if we're over size
        if (resultsTree.size() > resultSize) {
          resultsTree.remove(resultsTree.last());
        }
      }

    }
    
    return resultsTree;
  }
  

  private static abstract class DynamicCompositeComparator implements Comparator<ByteBuffer> {
    @SuppressWarnings("rawtypes")
    protected final AbstractType dynamicComposite;

    protected DynamicCompositeComparator(ApplicationCF cf) {
      // should never happen, this will blow up during development if this fails
      try {
        dynamicComposite = TypeParser.parse(cf.getComparator());
      } catch (ConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class DynamicCompositeForwardComparator extends DynamicCompositeComparator {

    /**
     * @param cf
     */
    protected DynamicCompositeForwardComparator(ApplicationCF cf) {
      super(cf);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compare(ByteBuffer o1, ByteBuffer o2) {
      return dynamicComposite.compare(o1, o2);
    }
  }

  private static class DynamicCompositeReverseComparator extends DynamicCompositeComparator {
    /**
     * @param cf
     */
    protected DynamicCompositeReverseComparator(ApplicationCF cf) {
      super(cf);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compare(ByteBuffer o1, ByteBuffer o2) {
      return dynamicComposite.compare(o2, o1);
    }
  }

}
