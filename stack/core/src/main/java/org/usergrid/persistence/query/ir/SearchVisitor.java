package org.usergrid.persistence.query.ir;

import java.util.Stack;

import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.cassandra.QueryProcessor;

/**
 * Simple search visitor that performs all the joining in memory for results.
 * 
 * Subclasses will want to implement visiting SliceNode and WithinNode to actually perform the search on the 
 * Cassandra indexes.  This class can perform joins on all index entries that conform to the Results object
 * 
 * @author tnine
 * 
 */
public abstract class SearchVisitor implements NodeVisitor {

    protected Query query;

    protected QueryProcessor queryProcessor;

    protected Stack<Results> results = new Stack<Results>();

    /**
     * @param query
     */
    public SearchVisitor(Query query, QueryProcessor queryProcessor) {
        this.query = query;
        this.queryProcessor = queryProcessor;
    }

    /**
     * Return the results if they exist, null otherwise
     * 
     * @return
     */
    public Results getResults() {
        return results.isEmpty() ? null : results.pop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.AndNode)
     */
    @Override
    public void visit(AndNode node) throws Exception {
        node.getLeft().visit(this);
        node.getRight().visit(this);

        Results right = results.pop();
        Results left = results.pop();

        left.and(right);

        results.push(left);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.NotNode)
     */
    @Override
    public void visit(NotNode node) throws Exception {
      node.getChild().visit(this);
      Results not = results.pop();

      node.getAllNode().visit(this);
      Results parent = results.pop();
      parent.subtract(not);

      results.push(parent);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.OrNode)
     */
    @Override
    public void visit(OrNode node) throws Exception {
        node.getLeft().visit(this);
        node.getRight().visit(this);

        Results right = results.pop();
        Results left = results.pop();

      if (left != null) {
        if (right != null) {
          left.merge(right);
        }
        results.push(left);
      } else {
        results.push(right);
      }
    }

}
