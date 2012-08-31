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

/**
 * @author tnine
 *
 */
public class NotNode extends QueryNode {

    protected QueryNode child, allNode;
    
    /**
     * @param child
     * @param allNode may be null if there are parents to this
     */
    public NotNode(QueryNode child, QueryNode allNode){
        this.child = child;
      this.allNode = allNode;
    }

    /**
     * @return the child
     */
    public QueryNode getChild() {
        return child;
    }

  /**
   * @return the all
   */
  public QueryNode getAllNode() {
    return allNode;
  }

  /* (non-Javadoc)
     * @see org.usergrid.persistence.query.ir.QueryNode#visit(org.usergrid.persistence.query.ir.NodeVisitor)
     */
    @Override
    public void visit(NodeVisitor visitor) throws Exception {
        visitor.visit(this);
    }

	@Override
	public String toString() {
		return "NotNode [child=" + child + "]";
	}




}
