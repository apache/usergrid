package org.apache.usergrid.perftest.rest;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.apache.usergrid.perftest.amazon.AmazonS3Service;
import org.apache.usergrid.perftest.amazon.Ec2Metadata;

import javax.ws.rs.core.MediaType;


/**
 * A resource that optionally propagates its operation to peer runners.
 */
public class PropagatingResource {

    private final String resourcePath;
    private final String endpointUrl;
    private final AmazonS3Service service;


    protected PropagatingResource( String resourcePath, AmazonS3Service service ) {
        this.resourcePath = resourcePath;
        this.service = service;
        this.endpointUrl = service.getMyMetadata().getUrl() + resourcePath;
    }


    /**
     * Gets the relative path to this resource.
     *
     * @return the relative path (minus hostname and port)
     */
    protected String getResourcePath() {
        return resourcePath;
    }


    /**
     * Gets the AmazonS3Service.
     *
     * @return the AmazonS3Service
     */
    protected AmazonS3Service getService() {
        return service;
    }


    /**
     * Gets the full http://hostname:port/foo/bar URL to the resource.
     *
     * @return the full resource URL
     */
    protected String getEndpointUrl() {
        return endpointUrl;
    }


    /**
     * Propagates this resource operation to other peers in the perftest cluster.
     *
     * @param status whether or not the propagating operation itself succeeded, sometimes you
     *               might want to propagate the operation even if the initiating operation failed
     * @param message the optional message to use if any
     * @return the results from the initiating peer and the remote peers.
     */
    protected PropagatedResult propagate( boolean status, String message ) {
        PropagatedResult result = new PropagatedResult( getEndpointUrl(), status, message );

        for ( String runner : getService().listRunners() )
        {
            Ec2Metadata metadata = getService().getRunner(runner);

            // skip if the runner is myself
            if ( getService().getMyMetadata().getPublicHostname().equals( metadata.getPublicHostname() ) )
            {
                continue;
            }

            DefaultClientConfig clientConfig = new DefaultClientConfig();
            Client client = Client.create( clientConfig );
            WebResource resource = client.resource( metadata.getUrl() );
            result.add( resource.path( getResourcePath() )
                    .queryParam( "propagate", "false" )
                    .accept( MediaType.APPLICATION_JSON_TYPE )
                    .post( BaseResult.class ) );
        }

        return result;
    }
}
