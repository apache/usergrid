package org.usergrid.persistence;

import java.util.Iterator;
import java.util.List;

/** iterates over a Results object, crossing page boundaries automatically */
public class PagingResultsIterator implements Iterator {

  private Results results;
  private Iterator currentPageIterator;
  private Results.Level level;

  public PagingResultsIterator(Results results) {
    this(results, results.level);
  }

  /** @param level overrides the default level from the Results - in case you want
   *               to return, say, UUIDs where the Query was set for Entities
   */
  public PagingResultsIterator(Results results, Results.Level level) {
    this.results = results;
    this.level = level;
    initCurrentPageIterator();
  }

  @Override
  public boolean hasNext() {
    if (currentPageIterator != null) {
      if (currentPageIterator.hasNext()) {
        return true;
      } else {
        return loadNextPage();
      }
    }
    return false;
  }

  /** @return the next object (type varies according the Results.Level) */
  @Override
  public Object next() {
    return currentPageIterator.next();
  }

  /** not supported */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  /** initialize the iterator for the current page of results.
   * @return true if the iterator has more results
   */
  private boolean initCurrentPageIterator() {
    List currentPage;
    if (results != null) {
      switch (level) {
        case IDS:
          currentPage = results.getIds();
          break;
        case REFS:
          currentPage = results.getRefs();
          break;
        default:
          currentPage = results.getEntities();
      }
      if (currentPage.size() > 0) {
        currentPageIterator = currentPage.iterator();
      }
    } else {
      currentPageIterator = null;
    }
    return currentPageIterator != null && currentPageIterator.hasNext();
  }

  /** attempts to load the next page
   * @return true if loaded there are more results
   */
  private boolean loadNextPage() {
    try {
      results = results.getNextPageResults();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return initCurrentPageIterator();
  }
}
