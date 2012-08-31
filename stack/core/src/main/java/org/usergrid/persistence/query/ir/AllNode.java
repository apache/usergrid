package org.usergrid.persistence.query.ir;

public class AllNode extends QueryNode {

  /* (non-Javadoc)
   * @see org.usergrid.persistence.query.ir.QueryNode#visit(org.usergrid.persistence.query.ir.NodeVisitor)
   */
  @Override
  public void visit(NodeVisitor visitor) throws Exception {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "AllNode";
  }
}
