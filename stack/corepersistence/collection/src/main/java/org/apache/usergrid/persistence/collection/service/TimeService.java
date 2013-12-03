package org.apache.usergrid.persistence.collection.service;


/** @author tnine */
public interface TimeService {

    /**
     * Get the current time in milliseconds since epoch
     * @return
     */
    long getTime();
}
