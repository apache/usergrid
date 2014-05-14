
package com.example.graphapp;


import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.guice.GraphModule;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Guice;
import com.google.inject.Injector;


public class Main2 {
   
    public static void main( String[] args ) {
        Injector injector = Guice.createInjector( new CollectionModule() );
        EntityCollectionManagerFactory cmf = injector.getInstance( EntityCollectionManagerFactory.class );


    }
}
