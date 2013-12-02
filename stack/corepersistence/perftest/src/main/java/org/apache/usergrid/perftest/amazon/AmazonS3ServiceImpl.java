/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.perftest.amazon;


import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * Sets up the S3 configuration if running in EC2.
 */
@Singleton
public class AmazonS3ServiceImpl implements AmazonS3Service {

    private final static Logger LOG = LoggerFactory.getLogger( AmazonS3ServiceImpl.class );
    private final static String DEFAULT_CONTAINER = "perftest-bucket";
    private static final Iterable<? extends Module> MODULES = ImmutableSet.of(
            new JavaUrlHttpCommandExecutorServiceModule(),
            new Log4JLoggingModule(),
            new NettyPayloadModule() );

    private DynamicStringProperty perftestFormation;

    private BlobStoreContext context;
    private boolean started = false;
    private String publicHostname;
    private String publicIpv4;
    private String localHostname;
    private String localIpv4;
    private String blobName;
    private DynamicStringProperty s3bucket;
    private DynamicStringProperty s3key;
    private DynamicStringProperty s3secret;


    public AmazonS3ServiceImpl()
    {
        s3bucket =
                DynamicPropertyFactory.getInstance().getStringProperty( AWS_BUCKET_KEY, DEFAULT_CONTAINER );
        s3key =
                DynamicPropertyFactory.getInstance().getStringProperty( AWS_KEY, "bogus" );
        s3secret =
                DynamicPropertyFactory.getInstance().getStringProperty( AWS_SECRET, "bogus" );
        perftestFormation =
                DynamicPropertyFactory.getInstance().getStringProperty( FORMATION_KEY, "default" );
    }


    @Override
    public void start()
    {
        if ( ! initialize() )
        {
            started = false;
            return;
        }

        System.setProperty( ARCHAIUS_CONTAINER_KEY, s3bucket.get() );
        started = true;
    }


    @Override
    public boolean isStarted()
    {
        return started;
    }


    @Override
    public void stop()
    {
        if ( isStarted() && context != null )
        {
            BlobStore blobStore = context.getBlobStore();
            blobStore.removeBlob( s3bucket.get(), blobName );
            context.close();
        }
    }


    private boolean initialize()
    {
        if ( ! populateEc2Metadata() )
        {
            LOG.warn( "We're not running in EC2! Node registration would not succeed." );
            return false;
        }

        StringBuilder sb = new StringBuilder();
        sb.append( perftestFormation.get() ).append( '_' ).append( publicHostname );
        this.blobName = sb.toString();

        sb = new StringBuilder();
        sb.append( FORMATION_KEY ).append( '=' ).append( perftestFormation.get() ).append( "\n" );
        sb.append( PUBLIC_HOSTNAME_KEY ).append( '=' ).append( publicHostname ).append("\n");
        sb.append( LOCAL_HOSTNAME_KEY ).append( '=' ).append( localHostname ).append( "\n" );
        sb.append( PUBLIC_IPV4_KEY ).append( '=' ).append( publicIpv4 ).append( "\n" );
        sb.append( LOCAL_IPV4_KEY ).append( '=' ).append( localIpv4 ).append( "\n" );

        context = ContextBuilder.newBuilder( "aws-s3" )
                .credentials( s3key.get(), s3secret.get() )
                .modules( MODULES )
                .buildView( BlobStoreContext.class );

        BlobStore blobStore = context.getBlobStore();
        BlobBuilder builder = blobStore.blobBuilder( this.blobName ).payload( sb.toString() );
        Blob blob = builder.build();
        String blobUuid = context.getBlobStore().putBlob( DEFAULT_CONTAINER, blob );
        LOG.info( "Successfully registered {} with UUID = {}", this.blobName, blobUuid );

        return true;
    }


    private boolean populateEc2Metadata()
    {
        try {
            this.publicHostname = extractEc2Metadata( PUBLIC_HOSTNAME_KEY );
            this.localHostname = extractEc2Metadata( LOCAL_HOSTNAME_KEY );
            this.publicIpv4 = extractEc2Metadata( PUBLIC_IPV4_KEY );
            this.localIpv4 = extractEc2Metadata( LOCAL_IPV4_KEY );
            return true;
        }
        catch ( IOException ioe ) {
            return false;
        }
    }


    private String extractEc2Metadata( String data ) throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append( INSTANCE_URL ).append( '/' ).append( data );
        
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet( url.toString() );

        try {
            HttpResponse response = client.execute( request );
            int statusCode = response.getStatusLine().getStatusCode();

            if ( statusCode == 200 ) {
                int initCapacity = ( int ) response.getEntity().getContentLength() + 1;
                ByteArrayOutputStream out = new ByteArrayOutputStream( initCapacity );
                response.getEntity().writeTo( out );
                out.flush();
                out.close();
                return out.toString();
            }

            throw new IOException( "Got bad status " + statusCode + " for URL " + url );
        }
        catch ( IOException ioe ) {
            LOG.error( "Failed to extract {} from {}", data, url );
            throw ioe;
        }
    }
}
