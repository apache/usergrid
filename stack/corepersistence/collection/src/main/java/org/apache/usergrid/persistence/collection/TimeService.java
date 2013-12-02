package org.apache.usergrid.persistence.collection;


/** @author tnine */
public interface TimeService {

    /**
     * Get the current time in milliseconds since epoch
     * @return
     */
    long getTime();
}
