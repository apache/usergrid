package org.usergrid.mq.cassandra.io;

import java.util.UUID;

public class QueueBounds {

    private final UUID oldest;
    private final UUID newest;

    public QueueBounds(UUID oldest, UUID newest) {
      this.oldest = oldest;
      this.newest = newest;
    }

    public UUID getOldest() {
      return oldest;
    }

    public UUID getNewest() {
      return newest;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = (prime * result) + ((newest == null) ? 0 : newest.hashCode());
      result = (prime * result) + ((oldest == null) ? 0 : oldest.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      QueueBounds other = (QueueBounds) obj;
      if (newest == null) {
        if (other.newest != null) {
          return false;
        }
      } else if (!newest.equals(other.newest)) {
        return false;
      }
      if (oldest == null) {
        if (other.oldest != null) {
          return false;
        }
      } else if (!oldest.equals(other.oldest)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "QueueBounds [oldest=" + oldest + ", newest=" + newest + "]";
    }
  }