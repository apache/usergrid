package org.apache.usergrid.rest;


import java.io.File;

import org.junit.rules.ExternalResource;
import org.apache.usergrid.cassandra.AvailablePortFinder;

import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.math.RandomUtils;

import com.google.common.io.Files;


/** @author tnine */
public class TomcatResource extends ExternalResource {

    private static Object mutex = new Object();

    private static final String CONTEXT = "/";

    private int port;




    private Tomcat tomcat;

    protected TomcatResource(){


    }

    @Override
    protected void before() throws Throwable {
        if ( tomcat != null ) {
            return;
        }

        synchronized ( mutex ) {
            //second into mutex
            if ( tomcat != null ) {
                return;
            }

            File dataDir = Files.createTempDir();
            dataDir.deleteOnExit();

            port = AvailablePortFinder.getNextAvailable( 9998 + RandomUtils.nextInt(10)  );

            tomcat = new Tomcat();
            tomcat.setBaseDir( dataDir.getAbsolutePath() );
            tomcat.setPort( port );
            tomcat.addWebapp( CONTEXT, new File( "src/main/webapp" ).getAbsolutePath() );


            tomcat.start();
        }
    }


    /**
     * Get the port tomcat runs on
     * @return
     */
    public int getPort(){
        return port;
    }


    public static final TomcatResource instance = new TomcatResource();


}
