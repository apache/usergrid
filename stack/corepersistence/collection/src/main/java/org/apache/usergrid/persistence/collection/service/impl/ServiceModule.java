package org.apache.usergrid.persistence.collection.service.impl;


import org.apache.usergrid.persistence.collection.service.UUIDService;

import com.google.inject.AbstractModule;


/**
 * @author tnine
 */
public class ServiceModule extends AbstractModule {

    @Override
    protected void configure() {

        //bind our uuid service
        bind( UUIDService.class ).to( UUIDServiceImpl.class );
    }
}
