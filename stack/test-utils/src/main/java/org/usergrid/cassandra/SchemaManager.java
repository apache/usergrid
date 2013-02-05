package org.usergrid.cassandra;

/**
 * @author zznate
 */
public interface SchemaManager {

    /**
     * Create any schema necessary for test execution.
     *
     */
    void create();

    /**
     *
     * @return true if the schema defined by this manager already exists
     */
    boolean exists();

    /**
     * Any breath-of-life data needed for the base system
     */
    void populateBaseData();

}
