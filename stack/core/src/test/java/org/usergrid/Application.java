package org.usergrid;


import org.junit.rules.TestRule;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

import java.util.UUID;


public interface Application extends TestRule
{
    UUID getId();

    String getOrgName();

    String getAppName();

    Entity create( String type ) throws Exception;

    Entity get( UUID id ) throws Exception;

    Object add( String property, Object value );

    void addToCollection( Entity user, String collection, Entity item ) throws Exception;

    Results searchCollection( Entity user, String collection, Query query ) throws Exception;

    void clear();
}
