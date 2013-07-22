package org.usergrid.persistence.geo.comparator;

import org.usergrid.persistence.geo.GeocellUtils;

public class EntityLocationComparableTuple<T> implements Comparable<EntityLocationComparableTuple<T>> {

  private T entity;
  private double distance;

  public EntityLocationComparableTuple(T entity, Double distance) {
    this.entity = entity;
    this.distance = distance;
  }

  @Override
  public int compareTo(EntityLocationComparableTuple<T> o) {
    if (o == null) {
      return -1;
    }
    int doubleCompare = Double.compare(distance, o.distance);

    if (doubleCompare == 0) {
      return GeocellUtils.getKey(entity).compareTo(GeocellUtils.getKey(entity));
    }

    return doubleCompare;

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
    @SuppressWarnings("unchecked")
    EntityLocationComparableTuple<T> other = (EntityLocationComparableTuple<T>) obj;
    if (entity == null) {
      if (other.entity != null) {
        return false;
      }
    } else if (!GeocellUtils.getKey(entity).equals(GeocellUtils.getKey(other.entity))) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return GeocellUtils.getKey(entity).hashCode();
  }

  /**
   * @return the entity
   */
  public T getEntity() {
    return entity;
  }

  /**
   * @return the distance
   */
  public double getDistance() {
    return distance;
  }

}
