package org.apache.usergrid.perftest.amazon;


import javax.servlet.ServletContext;
import java.util.Map;
import java.util.Set;


/**
 * The S3 Service is used to register the node so other nodes in the same
 * perftest formation can access it.
 */
public interface AmazonS3Service {

    void start();

    boolean isStarted();

    void stop();

    void triggerScan();

    Set<String> listRunners();

    Set<String> listTests();

    Ec2Metadata getRunner( String key );

    Map<String, Ec2Metadata> getRunners();

    void setServletContext( ServletContext context );

    Ec2Metadata getMyMetadata();

}
