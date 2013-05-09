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

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.UUID;

/**
 * Interface to parse and compare range slices
 * 
 * @author tnine
 *
 */
public interface SliceParser<T> extends Comparator<T> {

  /**
   * Parse the slice and return it's parse type
   * @param buff
   * @return
   */
  public T parse(ByteBuffer buff);
  
  /**
   * Get the UUID for the value
   * @param value
   * @return
   */
  public UUID getUUID(T value);
  
  /**
   * Serialize the parse type back into a byte buffer
   * @param type
   * @return
   */
  public ByteBuffer serialize(T type);
}
