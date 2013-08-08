package org.usergrid.persistence.query.ir;

import java.util.Stack;

import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.cassandra.QueryProcessor;
import org.usergrid.persistence.query.ir.result.*;

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

  protected final Query query;

  protected final QueryProcessor queryProcessor;

  protected final Stack<ResultIterator> results = new Stack<ResultIterator>();

  /**
   * @param queryProcessor
   */
  public SearchVisitor(QueryProcessor queryProcessor) {
    this.query = queryProcessor.getQuery();
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

    /**
     * NOTE: TN We should always maintain post order traversal of the tree.  It is required for sorting to work correctly 
     */
    IntersectionIterator intersection = new IntersectionIterator(queryProcessor.getPageSizeHint(node));
    intersection.addIterator(left);
    intersection.addIterator(right);
    
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
    node.getSubtractNode().visit(this);
    ResultIterator not = results.pop();

    node.getKeepNode().visit(this);
    ResultIterator keep = results.pop();
    
    SubtractionIterator subtraction = new SubtractionIterator(queryProcessor.getPageSizeHint(node));
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

    UnionIterator union = new UnionIterator(queryProcessor.getPageSizeHint(node));

    if (left != null) {
      union.addIterator(left);
    }
    if (right != null) {
      union.addIterator(right);
    }
    
    results.push(union);
  }

  @Override
  public void visit(UuidIdentifierNode uuidIdentifierNode) {
    this.results.push(new StaticIdIterator(uuidIdentifierNode.getUuid()));
  }



  @Override
  public void visit(EmailIdentifierNode emailIdentifierNode) throws Exception {
    EntityRef user = queryProcessor.getEntityManager().getUserByIdentifier(emailIdentifierNode.getIdentifier());

    if(user == null){
      this.results.push(new EmptyIterator());
      return;
    }

    this.results.push(new StaticIdIterator(user.getUuid()));
  }

}
