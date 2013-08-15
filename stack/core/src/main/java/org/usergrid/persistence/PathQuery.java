package org.usergrid.persistence;

import java.util.Iterator;
import java.util.UUID;

public class PathQuery<E> {

  private PathQuery source;
  private Query query;
  private UUID head;

  public PathQuery() {
  }

  /** top level
   * @param head the top-level entity
   */
  public PathQuery(EntityRef head) {
    this.head = head.getUuid();
    this.query = null;
  }

  /** top level
   * @param head the top-level entity
   * @param collectionName the query - must have a collection or connectType value set
   */
  public PathQuery(EntityRef head, Query query) {
    if (query.getCollection() == null && query.getConnectionType() == null) {
      throw new IllegalArgumentException("Query must have a collection or connectionType value");
    }
    this.head = head.getUuid();
    this.query = query;
  }

  /** chained
   * @param source the source query we're chaining from
   * @param collectionName the query - must have a collection or connectType value set
   */
  public PathQuery(PathQuery source, Query query) {
    if (query.getCollection() == null && query.getConnectionType() == null) {
      throw new IllegalArgumentException("Query must have a collection or connectionType value");
    }
    this.source = source;
    this.query = query;
  }

  public PathQuery chain(Query query) {
    return new PathQuery(this, query);
  }

  public Iterator<E> iterator(EntityManager em) {
    try {
      if (head != null) {
        return new PagingResultsIterator(getHeadResults(em), query.getResultsLevel());
      } else {
        return new MultiQueryIterator(em, source.uuidIterator(em), query);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  protected Results getHeadResults(EntityManager em) throws Exception {
    EntityRef ref = new SimpleEntityRef(head);
    return (query.getCollection() != null)
        ? em.searchCollection(ref, query.getCollection(), query)
        : em.searchConnectedEntities(ref, query);
  }

  protected Iterator uuidIterator(EntityManager em) throws Exception {
    if (head != null) {
      return new PagingResultsIterator(getHeadResults(em), Results.Level.IDS);
    } else {
      Query q = query;
      if (query.getResultsLevel() != Results.Level.IDS) { // ensure IDs level
        q = new Query(q);
        q.setResultsLevel(Results.Level.IDS);
      }
      return new MultiQueryIterator(em, source.uuidIterator(em), q);
    }
  }

  public PathQuery getSource() {
    return source;
  }

  public UUID getHead() {
    return head;
  }

  public Query getQuery() {
    return query;
  }
}
