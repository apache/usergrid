package org.apache.usergrid.persistence.query.ir;

import org.apache.usergrid.persistence.query.Query;

/**
 * Simple search visitor that builds query string. 
 */
public abstract class SearchVisitor implements NodeVisitor {
    protected final Query query;


    public SearchVisitor( Query query ) {
        this.query = query;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.
     * persistence.query.ir.AndNode)
     */
    @Override
    public void visit( AndNode node ) throws Exception {
        node.getLeft().visit( this );
        node.getRight().visit( this );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.
     * persistence.query.ir.NotNode)
     */
    @Override
    public void visit( NotNode node ) throws Exception {
        node.getSubtractNode().visit( this );
        node.getKeepNode().visit( this );

    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.
     * persistence.query.ir.OrNode)
     */
    @Override
    public void visit( OrNode node ) throws Exception {
        node.getLeft().visit( this );
        node.getRight().visit( this );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.persistence
     * .query.ir.OrderByNode)
     */
    @Override
    public void visit( OrderByNode orderByNode ) throws Exception {
    }

}
