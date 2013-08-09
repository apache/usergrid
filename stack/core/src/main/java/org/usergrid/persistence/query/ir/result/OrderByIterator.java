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
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityPropertyComparator;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.SortPredicate;
import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.cassandra.index.IndexScanner;
import org.usergrid.persistence.query.ir.QuerySlice;
import org.usergrid.persistence.query.util.PeekingIterator;

import java.nio.ByteBuffer;
import java.util.*;

import static org.usergrid.persistence.cassandra.IndexUpdate.compareIndexedValues;

/** @author tnine */
public class OrderByIterator extends MergeIterator {


  private static final Logger logger = LoggerFactory.getLogger(OrderByIterator.class);
  private final QuerySlice slice;
  private final IndexScanner firstOrder;
  private final SliceParser<DynamicComposite> parser;
  private final ComparatorChain subSortCompare;
  private final List<String> secondaryFields;
  private final EntityManager em;


  //the pointer to our last loaded set.  If we short circuited our loaded page, we'll want to resume from here
  private PeekingIterator<HColumn<ByteBuffer, ByteBuffer>> loadedPage;

  //our last result from in memory sorting
  private SortedEntitySet entries;


  /**
   * @param pageSize
   */
  public OrderByIterator(QuerySlice slice, IndexScanner firstOrder, SliceParser<DynamicComposite> parser,
                         List<Query.SortPredicate> secondary, EntityManager em, int pageSize) {
    super(pageSize);
    this.slice = slice;
    this.firstOrder = firstOrder;
    this.parser = parser;
    this.em = em;
    this.subSortCompare = new ComparatorChain();
    this.secondaryFields = new ArrayList<String>(1 + secondary.size());

    this.secondaryFields.add(slice.getPropertyName());

    for (SortPredicate sort : secondary) {
      this.subSortCompare.addComparator(new EntityPropertyComparator(sort.getPropertyName(),
          sort.getDirection() == Query
              .SortDirection.DESCENDING));
      this.secondaryFields.add(sort.getPropertyName());
    }


  }

  @Override
  protected Set<UUID> advance() {

    entries = new SortedEntitySet(subSortCompare, pageSize);

    Object lastValueInPreviousPage = null;

    boolean stopped = false;

    /**
     *  keep looping through our peek iterator.  We need to inspect each forward page to ensure we have performed a
     *  seek to the end of our primary range.  Otherwise we need to keep aggregating. I.E  if the value is a boolean and we order by "true
     *  asc, timestamp desc" we must load every entity that has the value "true" before sub sorting, then drop all values that fall out of the sort.
     */
    while (!stopped) {


      if(loadedPage == null){
        //we haven't loaded our first page, but we don't have any matches to load, return emtpy
        if(!firstOrder.hasNext()){
          break;
        }

        loadedPage = new PeekingIterator<HColumn<ByteBuffer, ByteBuffer>>(firstOrder.next().iterator());
      }
      //nothing loaded advanced based on our first order cass scan
      else if (!loadedPage.hasNext() && firstOrder.hasNext()) {
        loadedPage = new PeekingIterator<HColumn<ByteBuffer, ByteBuffer>>(firstOrder.next().iterator());
      }

      //our current set is empty and there's nothing to load further, exit
      if (!loadedPage.hasNext()) {
        return entries.toIds();
      }

      DynamicComposite composite = null;
      UUID id;

      Object currentValue = null;

      while (loadedPage.hasNext()) {
        HColumn<ByteBuffer, ByteBuffer> col = loadedPage.peek();

        composite = parser.parse(col.getName().duplicate());

        currentValue = parser.getValue(composite);

        /**
         * We've aggregated results already that are max size, and we've advanced to a "new" value, short circuit
         */
        if (lastValueInPreviousPage != null && entries.size() == pageSize && compareIndexedValues
            (lastValueInPreviousPage, currentValue) > 0) {
          stopped = true;
          break;
        }


        id = parser.getUUID(composite);

        entries.add(id, col.getName().duplicate());

        //pop what we processed
        loadedPage.next();
      }

      lastValueInPreviousPage = currentValue;

      //now load and sort the values, ones that we don't want will be dropped
      try {
        entries.load(em, secondaryFields);
      } catch (Exception e) {
        throw new RuntimeException("Unable to retrieve values for secondary sort", e);
      }

    }


    return entries.toIds();
  }

  @Override
  protected void doReset() {
    //reset our root cursor, we have to re-load everything anywa
    firstOrder.reset();
  }

  @Override
  public void finalizeCursor(CursorCache cache, UUID lastValue) {
    int sliceHash = slice.hashCode();

    ByteBuffer bytes = entries.getColumn(lastValue);

    if (bytes == null) {
      return;
    }

    cache.setNextCursor(sliceHash, bytes);
  }


  /** A Sorted set with a max size. When a new entry is added, the max is removed */
  public static final class SortedEntitySet extends TreeSet<Entity> {

    private final int maxSize;
    private final Set<UUID> toLoad = new LinkedHashSet<UUID>();
    private final Map<UUID, ByteBuffer> cursorVal = new HashMap<UUID, ByteBuffer>();


    public SortedEntitySet(Comparator<Entity> comparator, int maxSize) {
      super(comparator);
      this.maxSize = maxSize;
    }

    @Override
    public boolean add(Entity entity) {
      boolean added = super.add(entity);

      while (size() > maxSize) {
        //remove our last element, we're over size
        Entity e = this.pollLast();
        //remove it from the cursors as well
        cursorVal.remove(e.getUuid());
      }

      return added;
    }


    /** add the id to be loaded, and the dynamiccomposite column that belongs with it */
    public void add(UUID id, ByteBuffer column) {
      toLoad.add(id);
      cursorVal.put(id, column);
    }

    public void load(EntityManager em, List<String> fieldNames) throws Exception {
      for (Entity e : em.getPartialEntities(toLoad, fieldNames)) {
        add(e);
      }

    }

    /** Turn our sorted entities into a set of ids */
    public Set<UUID> toIds() {
      Set<UUID> ids = new LinkedHashSet<UUID>();

      Iterator<Entity> itr = iterator();

      while (itr.hasNext()) {
        ids.add(itr.next().getUuid());
      }

      return ids;
    }

    /** Get the dynamicComposite this id came from */
    public ByteBuffer getColumn(UUID id) {
      return cursorVal.get(id);
    }

  }


  protected static final Comparator<Object> entryComparator = new Comparator<Object>() {

    @Override
    public int compare(Object o1, Object o2) {
      return compareIndexedValues(o1, o2);
    }
  };

  protected static final UUIDComparator uuidComparator = new UUIDComparator();


}
