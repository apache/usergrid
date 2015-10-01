package org.apache.usergrid.persistence.collection.service;


import java.util.UUID;


/**
 * @author tnine
 */
public interface UUIDService {

    /**
     * Generate a new time uuid
     */
    UUID newTimeUUID();
}
