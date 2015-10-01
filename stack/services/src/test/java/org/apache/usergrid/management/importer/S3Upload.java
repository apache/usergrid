/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.management.importer;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.SDKGlobalConfiguration;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;


/**
 * Helper class made to upload resource files to s3 so they can be imported later
 */
public class S3Upload {

    private static final Logger logger = LoggerFactory.getLogger( S3Upload.class );

    public void copyToS3( String accessKey, String secretKey, String bucketName, List<String> filenames )
        throws FileNotFoundException {

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessKey );
        overrides.setProperty( "s3" + ".credential", secretKey );

        final Iterable<? extends Module> MODULES = ImmutableSet.of(
            new JavaUrlHttpCommandExecutorServiceModule(),
            new Log4JLoggingModule(),
            new NettyPayloadModule() );

        BlobStoreContext context = ContextBuilder.newBuilder( "s3" )
            .credentials( accessKey, secretKey )
            .modules( MODULES )
            .overrides( overrides )
            .buildView( BlobStoreContext.class );

        // Create Container (the bucket in s3)
        try {
            BlobStore blobStore = context.getBlobStore();
            if ( blobStore.createContainerInLocation( null, bucketName ) ) {
                logger.info( "Created bucket " + bucketName );
            }
        }
        catch ( Exception ex ) {
            throw new RuntimeException( "Could not create bucket",ex );
        }

        Iterator<String> fileNameIterator = filenames.iterator();

        while (fileNameIterator.hasNext()) {

            String filename = fileNameIterator.next();
            File uploadFile = new File( filename );

            try {
                BlobStore blobStore = context.getBlobStore();

                // need this for JClouds 1.7.x:
//                BlobBuilder.PayloadBlobBuilder blobBuilder =  blobStore.blobBuilder( filename )
//                    .payload( uploadFile ).calculateMD5().contentType( "application/json" );

                // needed for JClouds 1.8.x:
                BlobBuilder blobBuilder = blobStore.blobBuilder( filename )
                    .payload( uploadFile )
                    .contentMD5(Files.hash( uploadFile, Hashing.md5()))
                    .contentType( "application/json" );

                Blob blob = blobBuilder.build();

                final String uploadedFile = blobStore.putBlob( bucketName, blob, PutOptions.Builder.multipart() );

                //wait for the file to finish uploading
                Thread.sleep(4000);

                logger.info( "Uploaded file name={} etag={}", filename, uploadedFile );
            }
            catch ( Exception e ) {
                logger.error( "Error uploading to blob store. File: " + filename, e );
            }
        }
    }
}
