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

import java.util.Iterator;
import java.util.UUID;

import org.usergrid.persistence.cassandra.CursorCache;

/**
 * Interface for iterating slice results per node.  This is to be used to iterate and join or intersect values
 * Note that iteration is based on the order of the UUID of the entity so that the iterator
 * will iterate from min(UUID) to max(UUID)
 * 
 * @author tnine
 *
 */
public interface ResultIterator extends Iterable<UUID>, Iterator<UUID> {
  
  
  /**
   * Finalize the cursor for this results
   */
  public void finalizeCursor(CursorCache cache);

}
