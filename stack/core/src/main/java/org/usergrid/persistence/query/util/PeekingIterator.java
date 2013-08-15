package org.usergrid.persistence.query.util;

import java.util.Iterator;

/**
 * A simple iterator that allows us to "peek" to the next value without actually popping it.
 *
 * Meant as a wrapper to an existing iterator
 *
 * @author: tnine
 *
 */
public class PeekingIterator<T> implements Iterable<T>, Iterator<T> {

  private Iterator<T> delegate;
  private T peeked;

  public PeekingIterator(Iterator<T> delegate){
    this.delegate = delegate;
  }

  @Override
  public Iterator<T> iterator() {
    return this;
  }

  @Override
  public boolean hasNext() {
    return peeked != null || delegate.hasNext();
  }

  @Override
  public T next() {
    T toReturn = null;

    if(peeked != null){
      toReturn = peeked;
      peeked = null;
    }
    else{
      toReturn = delegate.next();
    }

    return toReturn;

  }

  /**
   * Peek ahead in the iterator.  Assumes a next is present and has been checked
   * @return
   */
  public T peek(){
    if(peeked == null && delegate.hasNext()){
      peeked = delegate.next();
    }
    return peeked;
  }


  @Override
  public void remove() {
   throw new UnsupportedOperationException("Remove is unsupported");
  }
}
