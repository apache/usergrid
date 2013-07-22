package org.usergrid.persistence.geo;

import java.util.List;

import org.usergrid.persistence.geo.model.GeocellQuery;

public interface GeocellQueryEngine {

	public abstract <T> List<T> query(GeocellQuery baseQuery, List<String> curGeocellsUnique, Class<T> entityClass);

}