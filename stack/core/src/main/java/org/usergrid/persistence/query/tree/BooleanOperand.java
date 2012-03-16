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

import org.antlr.runtime.Token;



/**
 * A base class for any equality expression.  Expressions must have a property and a value.  
 * Examples are >=, >, =, <, <=,
 *  
 * @author tnine
 *
 */
public abstract class BooleanOperand extends Operand {

  protected Token token;
  protected Operand left;
  protected Operand right;
  /**
   * @param property
   * @param literal
   */
  public BooleanOperand(Operand left, Token token, Operand right) {
    this.token = token;
    this.left = left;
    this.right = right;
  }
}