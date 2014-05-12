package org.apache.usergrid.persistence.collection.guice;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.GuicyFig;

import static org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity.Status;


/**
 *
 *
 */
public interface CollectionFig extends GuicyFig {

    @Default( "Complete" )
    Status getEntityState();

}
