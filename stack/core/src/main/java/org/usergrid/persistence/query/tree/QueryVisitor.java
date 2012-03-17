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

/**
 * I purposefully didn't create an interface for this visitor, we'll only every have 1 impl
 * @author tnine
 * 
 */
public class QueryVisitor {

  
  
  
  /**
   * 
   * @param op
   */
  public void visit(AndOperand op) {
    op.getLeft().visit(this);
    op.getRight().visit(this);
    
    //do op;
  }

  /**
   * @param op
   */
  public void visit(OrOperand op) {
    op.getLeft().visit(this);
    op.getRight().visit(this);
    
    //do op;
  }

  
  /**
   * @param op
   */
  public void visit(NotOperand op) {
    op.getOperation().visit(this);
    
    //intersect
  }
  
 

  /**
   * @param op
   */
  public void visit(LessThan op) {

  }


  /**
   * @param op
   */
  public void visit(ContainsOperand op) {

  }
  
  /**
   * @param op
   */
  public void visit(WithinOperand op) {

  }
  
  /**
   * @param op
   */
  public void visit(LessThanEqual op) {

  }
  
  /**
   * @param op
   */
  public void visit(Equal op) {

  }

  /**
   * @param op
   */
  public void visit(GreaterThan op) {

  }

  /**
   * @param op
   */
  public void visit(GreaterThanEqual op) {

  }
  

}
