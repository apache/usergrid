
package com.example.graphapp;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.guice.GraphModule;

public class Main {
   
    public static void main( String[] args ) {
        Injector injector = Guice.createInjector( new GraphModule() );
        GraphManagerFactory gmf = injector.getInstance( GraphManagerFactory.class );
    }
}
