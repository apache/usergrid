package org.apache.usergrid.persistence.collection.migration;


/**
 * Thrown when a migration cannot be performed
 *
 * @author tnine
 */
public class MigrationException extends Exception {

    public MigrationException( final String message ) {
        super( message );
    }


    public MigrationException( final String message, final Throwable cause ) {
        super( message, cause );
    }
}
