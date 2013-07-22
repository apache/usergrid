package org.usergrid.persistence.geo.comparator;

import org.usergrid.persistence.geo.model.LocationCapable;
import org.usergrid.persistence.geo.model.Tuple;

/**
 * This class is used to merge lists of Tuple<T, Double>. Lists are sorted following Double value but are equals only if T.key (same entity) are equals.
 *
 * @author Alexandre Gellibert
 *
 * @param <T>
 */
@Deprecated
public class LocationComparableTuple<T extends LocationCapable> extends Tuple<T ,Double> implements Comparable<LocationComparableTuple<T>>{

    public LocationComparableTuple(T first, Double second) {
        super(first, second);
    }

    public int compareTo(LocationComparableTuple<T> o) {
        if(o == null) {
            return -1;
        }
        int doubleCompare = this.getSecond().compareTo(o.getSecond());
        if(doubleCompare == 0) {
            return this.getFirst().getKeyString().compareTo(o.getFirst().getKeyString());
        } else {
            return doubleCompare;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
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
        LocationComparableTuple<LocationCapable> other = (LocationComparableTuple<LocationCapable>) obj;
        if (getFirst() == null) {
            if (other.getFirst() != null) {
                return false;
            }
        } else if (!getFirst().getKeyString().equals(other.getFirst().getKeyString())) {
            return false;
        }
        return true;
    }


    @Override
    public int hashCode() {
        return getFirst().getKeyString().hashCode();
    }

}