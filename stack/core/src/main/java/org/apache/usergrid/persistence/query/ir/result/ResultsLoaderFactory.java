package org.apache.usergrid.persistence.query.ir.result;


import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;


/**
 *
 * @author: tnine
 *
 */
public interface ResultsLoaderFactory {

    /**
     * Get the results loaded that will load all Ids given the results level.  The original query and the entity manager
     * may be needed to load these results
     */
    public ResultsLoader getResultsLoader( EntityManager em, Query query, Results.Level level );
}
