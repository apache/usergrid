package org.usergrid.persistence.geo.comparator;

import java.util.Comparator;

import org.usergrid.persistence.geo.model.Tuple;


public class DoubleTupleComparator implements Comparator<Tuple<int[], Double>> {

	public int compare(Tuple<int[], Double> o1, Tuple<int[], Double> o2) {
		if(o1 == null && o2 == null) {
			return 0;
		}
		if(o1 == null) {
			return -1;
		}
		if(o2 == null) {
			return 1;
		}
		return o1.getSecond().compareTo(o2.getSecond());
	}

}
