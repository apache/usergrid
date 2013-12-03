package org.apache.usergrid.persistence.collection.service.impl;


import org.apache.usergrid.persistence.collection.service.TimeService;


/** @author tnine */
public class TimeServiceImpl implements TimeService {

    @Override
    public long getTime() {
        return System.currentTimeMillis();
    }
}
