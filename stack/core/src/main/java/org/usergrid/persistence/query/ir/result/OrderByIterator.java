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

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.collect.TreeMultimap;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityPropertyComparator;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.SortPredicate;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.cassandra.index.IndexScanner;
import org.usergrid.persistence.query.ir.QuerySlice;

import java.nio.ByteBuffer;
import java.util.*;

import static org.usergrid.persistence.cassandra.IndexUpdate.compareIndexedValues;

/** @author tnine */
public class OrderByIterator extends MergeIterator {

  private final QuerySlice slice;
  private final IndexScanner firstOrder;
  private final SliceParser<DynamicComposite> parser;
  private final ComparatorChain subSortCompare;
//  private final List<SortPredicate> secondary;


  /**
   * @param pageSize
   */
  public OrderByIterator(QuerySlice slice, IndexScanner firstOrder, SliceParser<DynamicComposite> parser,
                         List<Query.SortPredicate> secondary, int pageSize) {
    super(pageSize);
    this.slice = slice;
    this.firstOrder = firstOrder;
    this.parser = parser;
    subSortCompare = new ComparatorChain();
    for(SortPredicate sort: secondary){
      subSortCompare.addComparator(new EntityPropertyComparator(sort.getPropertyName(), sort.getDirection() == Query
          .SortDirection.DESCENDING));
    }
  }

  @Override
  protected Set<UUID> advance() {


    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void doReset() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void finalizeCursor(CursorCache cache, UUID lastValue) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  private void sort(){
    TreeMultimap<Object, UUID> entities = buildOrder();

    Set<Entity> results = new LinkedHashSet<Entity>(pageSize);

    EntityManager em;


    //TODO T.N.  Perform a multiget to load all secondary sorts in 1 pass from cassandra

    for(Object key : entities.keySet()){


    }


  }



  private TreeMultimap<Object, UUID> buildOrder() {
    TreeMultimap<Object, UUID> groups = TreeMultimap.create(entryComparator, uuidComparator);


    if (!firstOrder.hasNext()) {
      return groups;
    }


    DynamicComposite composite = null;
    Object value;
    UUID id;


    for (HColumn<ByteBuffer, ByteBuffer> col : firstOrder.next()) {
      composite = parser.parse(col.getName().duplicate());

      value = parser.getValue(composite);
      id = parser.getUUID(composite);

      groups.put(value, id);

    }

    return groups;

  }

  protected static final Comparator<Object> entryComparator = new Comparator<Object>() {

    @Override
    public int compare(Object o1, Object o2) {
      return compareIndexedValues(o1, o2);
    }
  };

  protected static final UUIDComparator uuidComparator = new UUIDComparator();

}
