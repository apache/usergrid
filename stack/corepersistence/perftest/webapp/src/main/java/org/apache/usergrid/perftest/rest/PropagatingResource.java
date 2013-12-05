package org.apache.usergrid.perftest.rest;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.apache.usergrid.perftest.amazon.AmazonS3Service;
import org.apache.usergrid.perftest.amazon.Ec2Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;


/**
 * A resource that optionally propagates its operation to peer runners.
 */
public class PropagatingResource {
    private static final Logger LOG = LoggerFactory.getLogger( PropagatingResource.class );

    private final String resourcePath;
    private final String endpointUrl;
    private final ExecutorService executorService;
    private final AmazonS3Service service;


    protected PropagatingResource( String resourcePath, AmazonS3Service service ) {
        this.resourcePath = resourcePath;
        this.service = service;
        this.endpointUrl = service.getMyMetadata().getUrl() + resourcePath;
        executorService = Executors.newCachedThreadPool();
    }


    /**
     * Although this super class method Provides an optional recovery operation that can be executed when
     * propagation calls fail. The PropagationResource will only apply
     * recovery tactics if a non-null recovery operation is available.
     *
     * A good example when this mechanism is used is the /load operation
     * which will more often fail rather than succeed because when
     * restarting the application and responding there exists a race
     * condition. This recovery operation can be used to check that the
     * operation succeeded properly.
     *
     * @return a schedulable job to be performed on failure, or null
     */
    protected Callable<Result> getRecoveryOperation( @SuppressWarnings( "UnusedParameters" )
                                                     final PropagatingCall failingCaller )
    {
        return null;
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
        return propagate( status, message, Collections.<String,String>emptyMap() );
    }


    /**
     * Propagates this resource operation to other peers in the perftest cluster.
     *
     * @param status whether or not the propagating operation itself succeeded, sometimes you
     *               might want to propagate the operation even if the initiating operation failed
     * @param message the optional message to use if any
     * @param params additional query parameters to pass-through to peers being propagated to
     * @return the results from the initiating peer and the remote peers.
     */
    protected PropagatedResult propagate( boolean status, String message, final Map<String,String> params ) {
        PropagatedResult result = new PropagatedResult( getEndpointUrl(), status, message );
        BlockingQueue<Future<Result>> completionQueue = new LinkedBlockingQueue<Future<Result>>();
        ExecutorCompletionService<Result> completionService =
                new ExecutorCompletionService<Result>( executorService, completionQueue );

        for ( String runner : getService().listRunners() )
        {
            final Ec2Metadata metadata = getService().getRunner( runner );

            // skip if the runner is myself
            if ( getService().getMyMetadata().getPublicHostname().equals( metadata.getPublicHostname() ) ) {
                continue;
            }

            completionService.submit( new PropagatingCall( metadata, params ) );
        }

        while ( ! completionQueue.isEmpty() ) {
            try {
                Future<Result> future = completionService.poll( 200, TimeUnit.MILLISECONDS );

                if ( future.isDone() || future.isCancelled() ) {
                    result.add( future.get() );
                }
            }
            catch ( InterruptedException e ) {
                LOG.error( "Interrupted while polling completionService.", e );
            }
            catch ( ExecutionException e ) {
                LOG.error( "Failure accessing Result from Future.", e );
            }
        }

        return result;
    }


    class PropagatingCall implements Callable<Result>
    {
        private final Ec2Metadata metadata;
        private final Map<String,String> params;

        PropagatingCall( Ec2Metadata metadata, Map<String,String> params ) {
            this.metadata = metadata;
            this.params = params;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        Ec2Metadata getMetadata() {
            return metadata;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        Map<String,String> getParams() {
            return params;
        }

        @Override
        public Result call() throws Exception {
            DefaultClientConfig clientConfig = new DefaultClientConfig();
            Client client = Client.create( clientConfig );
            WebResource resource = client.resource( metadata.getUrl() ).path( getResourcePath() );

            // Inject required query parameters
            resource = resource.queryParam( "propagate", "false" );
            for ( String paramKey : params.keySet() ) {
                if ( paramKey.equals( "propagate" ) ) {
                    continue;
                }

                resource = resource.queryParam( paramKey, params.get( paramKey ) );
            }

            Result remoteResult;

            try {
                remoteResult = resource.accept( MediaType.APPLICATION_JSON_TYPE ).post( BaseResult.class );
            }
            catch ( Exception e ) {
                LOG.error( "Failure on post to peer {}.", metadata.getPublicHostname() );

                Callable<Result> recoveryOp = getRecoveryOperation( this );

                if ( recoveryOp == null )
                {
                    throw e;
                }

                try {
                    return recoveryOp.call();
                }
                catch ( Exception e2 ) {
                    LOG.error( "Failures encountered on recovery operation. Considering " +
                            "this propagating call to be a failure." );
                    return new BaseResult( getEndpointUrl(), false,
                            "Multiple failures encountered including on recovery operation!" );
                }
            }

            return remoteResult;
        }
    }
}
