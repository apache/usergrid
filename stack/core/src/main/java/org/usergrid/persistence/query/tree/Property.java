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

import org.antlr.runtime.ClassicToken;
import org.antlr.runtime.Token;

/**
 * A property
 * 
 * @author tnine
 * 
 */
public class Property extends Literal<String> {

  private String property;

  public Property(Token t) {
    super(t);
    this.property = t.getText();
  }
  
  public Property(String property){
    super(new ClassicToken(0, property));
    this.property = property;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.tree.Literal#getValue()
   */
  @Override
  public String getValue() {
    return this.property;
  }

}
