package org.apache.usergrid.persistence.collection.service.impl;


import org.apache.usergrid.persistence.collection.service.TimeService;
import org.apache.usergrid.persistence.collection.service.UUIDService;

import com.google.inject.AbstractModule;


/** @author tnine */
public class ServiceModule extends AbstractModule{

    @Override
    protected void configure() {

        //bind our keyspace to the AstynaxKeyspaceProvider
        bind( TimeService.class ).to( TimeServiceImpl.class );

        //bind our migration manager
        bind( UUIDService.class ).to( UUIDServiceImpl.class );


    }
}
