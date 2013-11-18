package org.usergrid.persistence.query.ir.result;


import org.usergrid.persistence.ConnectionRef;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;


/** Implementation for loading connectionResults results */
public class ConnectionResultsLoaderFactory implements ResultsLoaderFactory {

    private final ConnectionRef connection;


    public ConnectionResultsLoaderFactory( ConnectionRef connection ) {
        this.connection = connection;
    }


    @Override
    public ResultsLoader getResultsLoader( EntityManager em, Query query, Results.Level level ) {
        switch ( level ) {
            case IDS://Note that this is technically wrong.  However, to support backwards compatibility with the
                // existing apis and usage, both ids and refs return a connection ref when dealing with connections
            case REFS:
                return new ConnectionRefLoader( connection );
            default:
                return new EntityResultsLoader( em );
        }
    }
}
