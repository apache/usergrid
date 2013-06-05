package org.usergrid.persistence.query.ir;

/**
 * Used to represent a "select all".  This will iterate over the entities by UUID
 * @author tnine
 *
 */
public class AllNode extends QueryNode {


  private final QuerySlice slice;
  
  
  /**
   * Note that the slice isn't used on select, but is used when creating cursors
   * @param id.  The unique numeric id for this node
   */
  public AllNode(int id){
    this.slice = new QuerySlice("uuid", id);
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
    return "AllNode";
  }

  /**
   * @return the slice
   */
  public QuerySlice getSlice() {
    return slice;
  }
}
