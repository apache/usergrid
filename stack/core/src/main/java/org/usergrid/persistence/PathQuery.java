package org.usergrid.persistence;

import java.util.Iterator;

/**
 * iterator() will be a PagingResultsIterator()
 */
public class PathQuery<E> implements Iterable<E> {

  private EntityManager em;
  private PathQuery source;
  private Query query;
  private String collectionName;
  private Results head;

  /** top level
   * @param collectionName null if connection query
   */
  public PathQuery(Results results) {
    head = results;
    em = results.getQueryProcessor().getEntityManager();
  }

  /** chained
   * @param collectionName null if connection query
   */
  public PathQuery(EntityManager em, PathQuery source, String collectionName, Query query) {
    this.em = em;
    this.source = source;
    this.collectionName = collectionName;
    this.query = query;
  }

  public PathQuery chainCollectionQuery(String collectionName, Query query) {
    return new PathQuery(em, this, collectionName, query);
  }

  public PathQuery chainConnectionQuery(Query query) {
    return new PathQuery(em, this, null, query);
  }

  protected Iterator uuidIterator() throws Exception {
    if (head != null) {
      return new PagingResultsIterator(head, Results.Level.IDS);
    } else {
      Query q = query;
      if (query.getResultsLevel() != Results.Level.IDS) {
        q = new Query(q);
        q.setResultsLevel(Results.Level.IDS);
      }
      return new MultiQueryIterator(em, source.uuidIterator(), q, collectionName);
    }
  }

  public Iterator<E> iterator() {
    try {
      if (head != null) {
        return new PagingResultsIterator(head, query.getResultsLevel());
      } else {
        return new MultiQueryIterator(em, source.uuidIterator(), query, collectionName);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
