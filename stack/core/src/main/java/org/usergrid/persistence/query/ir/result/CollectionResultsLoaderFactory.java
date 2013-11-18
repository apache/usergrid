package org.usergrid.persistence.query.ir.result;


import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;


/** Implementation for loading collection results */
public class CollectionResultsLoaderFactory implements ResultsLoaderFactory {

    @Override
    public ResultsLoader getResultsLoader( EntityManager em, Query query, Results.Level level ) {
        switch ( level ) {
            case IDS:
                return new IDLoader();
            case REFS:
                return new EntityRefLoader( query.getEntityType() );
            default:
                return new EntityResultsLoader( em );
        }
    }
}
