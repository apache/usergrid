package org.apache.usergrid.persistence.exceptions;


/**
 * Thrown when an error occurs during query iteration
 *
 * @author tnine
 */
public class QueryIterationException extends RuntimeException {
    public QueryIterationException( final String message ) {
        super( message );
    }
}
