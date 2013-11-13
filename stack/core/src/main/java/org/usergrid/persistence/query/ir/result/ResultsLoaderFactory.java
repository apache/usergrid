package org.usergrid.persistence.query.ir.result;


import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;


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
