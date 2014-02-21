package org.apache.usergrid.persistence.query.ir;


/**
 * Used to represent a "select all".  This will iterate over the entities by UUID
 *
 * @author tnine
 */
public class AllNode extends QueryNode {


    private final boolean forceKeepFirst;


    /**
     * Note that the slice isn't used on select, but is used when creating cursors
     *
     * @param id. The unique numeric id for this node
     * @param forceKeepFirst True if we don't allow the iterator to skip the first result, regardless of cursor state.
     * Used for startUUID paging
     */
    public AllNode( int id, boolean forceKeepFirst ) {
        this.forceKeepFirst = forceKeepFirst;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.QueryNode#visit(org.apache.usergrid.persistence.query.ir.NodeVisitor)
     */
    @Override
    public void visit( NodeVisitor visitor ) throws Exception {
        visitor.visit( this );
    }


    @Override
    public String toString() {
        return "AllNode";
    }

    /** @return the skipFirstMatch */
    public boolean isForceKeepFirst() {
        return forceKeepFirst;
    }
}
