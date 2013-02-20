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
package org.usergrid.mq.cassandra.io;

import org.usergrid.mq.QueueQuery;
import org.usergrid.mq.QueueResults;

/**
 * Searches in the queue without transactions
 * 
 * @author tnine
 *
 */
public class BooleanSearch implements QueueSearch {

  /**
   * 
   */
  public BooleanSearch() {
  }

  /* (non-Javadoc)
   * @see org.usergrid.mq.cassandra.io.QueueSearch#getResults(java.lang.String, org.usergrid.mq.QueueQuery)
   */
  @Override
  public QueueResults getResults(String queuePath, QueueQuery query) {
  return null;
    
//    QueryProcessor qp = new QueryProcessor(query);
//  List<QuerySlice> slices = qp.getSlices();
//
//  
//  
//  TreeSet<UUID> prev_set = null;
//
//  if (prev_count > 0) {
//    if (slices.size() > 1) {
//      prev_count = DEFAULT_SEARCH_COUNT;
//    }
//
//    for (QuerySlice slice : slices) {
//
//      TreeSet<UUID> r = null;
//      try {
//        r = searchQueueRange(ko, queueId, bounds, null,
//            start_uuid, null, true, slice, prev_count);
//      } catch (Exception e) {
//        logger.error("Error during search", e);
//      }
//
//      if (prev_set != null) {
//        mergeAnd(prev_set, r, true, 10000);
//      } else {
//        prev_set = r;
//      }
//
//    }
//  }
//
//  TreeSet<UUID> next_set = null;
//
//  if (slices.size() > 1) {
//    next_count = DEFAULT_SEARCH_COUNT;
//  }
//
//  for (QuerySlice slice : slices) {
//
//    TreeSet<UUID> r = null;
//    try {
//      r = searchQueueRange(ko, queueId, bounds, null, start_uuid,
//          null, false, slice, skip_first ? next_count + 1
//              : next_count);
//    } catch (Exception e) {
//      logger.error("Error during search", e);
//    }
//
//    if (next_set != null) {
//      mergeAnd(next_set, r, true, 10000);
//    } else {
//      next_set = r;
//    }
//
//  }
  }


 
}
