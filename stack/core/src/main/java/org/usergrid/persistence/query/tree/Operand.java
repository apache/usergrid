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

import org.antlr.runtime.tree.CommonTree;

import org.antlr.runtime.Token;

/**
 * Any logical operation should subclass.  Boolean logic, equality, not, contains, within and others are examples of operands
 * 
 * @author tnine
 *
 */
public abstract class Operand extends CommonTree{

  
  /**
   * Default constructor to take a token
   * @param t
   */
  public Operand(Token t){
    super(t);
  }
  
  /**
   * Visitor method
   * @param visitor
   */
  public abstract void visit(QueryVisitor visitor);
  
  
}
