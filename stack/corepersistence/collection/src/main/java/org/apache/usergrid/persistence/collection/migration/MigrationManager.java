package org.apache.usergrid.persistence.collection.migration;


/**
 * A manager that will perform any migrations necessary.  Setup code should invoke the implementation of this interface
 *
 * @author tnine
 */
public interface MigrationManager {

    /**
     * Perform any migration necessary in the application.  Will only create keyspaces and column families if they do
     * not exist
     */
    public void migrate() throws MigrationException;
}
