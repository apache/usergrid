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
import java.util.UUID;

import org.usergrid.persistence.Schema;

import me.prettyprint.hector.api.beans.DynamicComposite;

/**
 * Parser for reading uuid connections from ENTITY_COMPOSITE_DICTIONARIES and DICTIONARY_CONNECTED_ENTITIES type
 * 
 * @author tnine
 *
 */
public class ConnectionIndexSliceParser implements SliceParser<DynamicComposite> {

  private final String connectedEntityType;
  /**
   * @param connectedEntityType
   */
  public ConnectionIndexSliceParser(String connectedEntityType) {
    this.connectedEntityType = connectedEntityType;
  }


  /* (non-Javadoc)
   * @see org.usergrid.persistence.query.ir.result.SliceParser#parse(java.nio.ByteBuffer)
   */
  @Override
  public DynamicComposite parse(ByteBuffer buff) {
    DynamicComposite composite = DynamicComposite.fromByteBuffer(buff.duplicate());
    
    String connectedType = (String) composite.get(1);
    
    
    //connection type has been defined and it doesn't match, skip it
    if(connectedEntityType != null &&  !connectedEntityType.equals(connectedType)){
      return null;
    }
    
    //we're checking a loopback, skip it
    if(Schema.TYPE_CONNECTION.equalsIgnoreCase(connectedType)){
      return null;
    }
    
    
    
    return composite;
  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.query.ir.result.SliceParser#getUUID(java.lang.Object)
   */
  @Override
  public UUID getUUID(DynamicComposite value) {
    return (UUID) value.get(0);
  }

  @Override
  public Object getValue(DynamicComposite value) {
    throw new UnsupportedOperationException("Getting the value is not supported on connections");
  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.query.ir.result.SliceParser#serialize(java.lang.Object)
   */
  @Override
  public ByteBuffer serialize(DynamicComposite type) {
    return type.serialize();
  }

}
