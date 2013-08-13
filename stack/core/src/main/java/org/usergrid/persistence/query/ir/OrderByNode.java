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
package org.usergrid.persistence.query.ir;

import java.util.List;

import org.usergrid.persistence.Query.SortPredicate;

/**
 * Intermediate representation of ordering operations
 * 
 * @author tnine
 * 
 */
public class OrderByNode extends QueryNode {

  private final SliceNode firstPredicate;
	private final List<SortPredicate> secondarySorts;

  /**
   *
   * @param firstPredicate The first predicate that is in the order by statement
   * @param secondarySorts Any subsequent terms
   */
	public OrderByNode(SliceNode firstPredicate, List<SortPredicate> secondarySorts) {
	  this.firstPredicate = firstPredicate;
	  this.secondarySorts = secondarySorts;
	}


  /**
   * @return the sorts
   */
  public List<SortPredicate> getSecondarySorts() {
    return secondarySorts;
  }


  /**
   * @return the firstPredicate
   */
  public SliceNode getFirstPredicate() {
    return firstPredicate;
  }


  /*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.usergrid.persistence.query.ir.QueryNode#visit(org.usergrid.persistence
	 * .query.ir.NodeVisitor)
	 */
	@Override
	public void visit(NodeVisitor visitor) throws Exception {
		visitor.visit(this);
	}

  /**
   * Return true if this order has secondary sorts
   * @return
   */
  public boolean hasSecondarySorts(){
    return secondarySorts != null && secondarySorts.size() > 0;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "OrderByNode [sorts=" + secondarySorts + "]";
  }



}
