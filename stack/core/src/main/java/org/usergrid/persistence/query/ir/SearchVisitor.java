package org.usergrid.persistence.query.ir;

import java.util.Stack;

import org.usergrid.persistence.Query;
import org.usergrid.persistence.cassandra.QueryProcessor;
import org.usergrid.persistence.cassandra.RelationManagerImpl;
import org.usergrid.persistence.query.ir.result.IntersectionIterator;
import org.usergrid.persistence.query.ir.result.ResultIterator;
import org.usergrid.persistence.query.ir.result.SubtractionIterator;
import org.usergrid.persistence.query.ir.result.UnionIterator;

/**
 * Simple search visitor that performs all the joining in memory for results.
 * 
 * Subclasses will want to implement visiting SliceNode and WithinNode to
 * actually perform the search on the Cassandra indexes. This class can perform
 * joins on all index entries that conform to the Results object
 * 
 * @author tnine
 * 
 */
public abstract class SearchVisitor implements NodeVisitor {

  protected Query query;

  protected QueryProcessor queryProcessor;

  protected Stack<ResultIterator> results = new Stack<ResultIterator>();

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
  public ResultIterator getResults() {
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

    ResultIterator right = results.pop();
    ResultIterator left = results.pop();

    IntersectionIterator intersection = new IntersectionIterator(RelationManagerImpl.PAGE_SIZE);
    intersection.addIterator(right);
    intersection.addIterator(left);
    
    results.push(intersection);
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
    ResultIterator not = results.pop();

    node.getAllNode().visit(this);
    ResultIterator keep = results.pop();
    
    SubtractionIterator subtraction = new SubtractionIterator(RelationManagerImpl.PAGE_SIZE);
    subtraction.setSubtractIterator(not);
    subtraction.setKeepIterator(keep);

    results.push(subtraction);
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

    ResultIterator right = results.pop();
    ResultIterator left = results.pop();

    UnionIterator union = new UnionIterator(RelationManagerImpl.PAGE_SIZE);

    if (left != null) {
      union.addIterator(left);
    }
    if (right != null) {
      union.addIterator(right);
    }
    
    results.push(union);
  }

}
