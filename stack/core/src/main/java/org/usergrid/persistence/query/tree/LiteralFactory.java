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
package org.usergrid.persistence.query.tree;

import java.util.UUID;

/**
 * Simple factory for generating literal instance based on the runtime value
 * @author tnine
 *
 */
public class LiteralFactory {

  /**
   * Generate the correct literal subclass based on the runtime instance.
   * @param value
   * @return
   */
  public static final Literal<?> getLiteral(Object value){
    if(value instanceof Integer){
      return new IntegerLiteral((Integer)value);
    }
    
    if(value instanceof String){
      return new StringLiteral((String) value);
    }
    
    if(value instanceof Float){
      return new FloatLiteral((Float)value);
    }
    
    if(value instanceof UUID){
      return new UUIDLiteral((UUID)value);
    }
    
    if(value instanceof Boolean){
        return new BooleanLiteral((Boolean)value);
    }
    
    throw new UnsupportedOperationException(String.format("Unsupported type of %s was passed when trying to construct a literal", value.getClass()));
    
  }
}
