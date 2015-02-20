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
package org.apache.usergrid.services.assets.data;


import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.AsyncBlobStore;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.options.GetOptions;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Module;


public class S3BinaryStore implements BinaryStore {

    private static final Iterable<? extends Module> MODULES = ImmutableSet
            .of( new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(), new NettyPayloadModule() );

    private static final Logger LOG = LoggerFactory.getLogger( S3BinaryStore.class );
    private static final long FIVE_MB = ( FileUtils.ONE_MB * 5 );

    private BlobStoreContext context;
    private String accessId;
    private String secretKey;
    private String bucketName;
    private ExecutorService executor = Executors.newFixedThreadPool( 10 );

    @Autowired
    private EntityManagerFactory emf;


    public S3BinaryStore( String accessId, String secretKey, String bucketName ) {
        this.accessId = accessId;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
    }


    private BlobStoreContext getContext() {
        if ( context == null ) {
            context = ContextBuilder.newBuilder( "aws-s3" ).credentials( accessId, secretKey ).modules( MODULES )
                                    .buildView( BlobStoreContext.class );

            BlobStore blobStore = context.getBlobStore();
            blobStore.createContainerInLocation( null, bucketName );
        }

        return context;
    }


    public void destroy() {
        if ( context != null ) {
            context.close();
        }
    }


    @Override
    public void write( final UUID appId, final Entity entity, InputStream inputStream ) throws IOException {

        String uploadFileName = AssetUtils.buildAssetKey( appId, entity );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long written = IOUtils.copyLarge( inputStream, baos, 0, FIVE_MB );
        byte[] data = baos.toByteArray();

        final Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( entity );
        fileMetadata.put( AssetUtils.LAST_MODIFIED, System.currentTimeMillis() );

        String mimeType = AssetMimeHandler.get().getMimeType( entity, data );

        if ( written < FIVE_MB ) { // total smaller than 5mb

            BlobStore blobStore = getContext().getBlobStore();
            BlobBuilder.PayloadBlobBuilder bb = blobStore.blobBuilder(uploadFileName)
                .payload(data)
                .contentMD5(Hashing.md5().newHasher().putBytes( data ).hash())
                .contentType(mimeType);

            fileMetadata.put( AssetUtils.CONTENT_LENGTH, written );
            if ( fileMetadata.get( AssetUtils.CONTENT_DISPOSITION ) != null ) {
                bb.contentDisposition( fileMetadata.get( AssetUtils.CONTENT_DISPOSITION ).toString() );
            }
            final Blob blob = bb.build();

            String md5sum = Hex.encodeHexString( blob.getMetadata().getContentMetadata().getContentMD5() );
            fileMetadata.put( AssetUtils.CHECKSUM, md5sum );

            String eTag = blobStore.putBlob( bucketName, blob );
            fileMetadata.put( AssetUtils.E_TAG, eTag );
        }
        else { // bigger than 5mb... dump 5 mb tmp files and upload from them

            // todo: yes, AsyncBlobStore is deprecated, but there appears to be no replacement yet
            final AsyncBlobStore blobStore = getContext().getAsyncBlobStore();

            File tempFile = File.createTempFile( entity.getUuid().toString(), "tmp" );
            tempFile.deleteOnExit();
            OutputStream os = null;
            try {
                os = new BufferedOutputStream( new FileOutputStream( tempFile.getAbsolutePath() ) );
                os.write( data );
                written += IOUtils.copyLarge( inputStream, os, 0, ( FileUtils.ONE_GB * 5 ) );
            }
            finally {
                IOUtils.closeQuietly( os );
            }

            BlobBuilder.PayloadBlobBuilder bb = blobStore.blobBuilder( uploadFileName )
                .payload(tempFile)
                .contentMD5(Files.hash(tempFile, Hashing.md5()))
                .contentType(mimeType);

            fileMetadata.put( AssetUtils.CONTENT_LENGTH, written );
            if ( fileMetadata.get( AssetUtils.CONTENT_DISPOSITION ) != null ) {
                bb.contentDisposition( fileMetadata.get( AssetUtils.CONTENT_DISPOSITION ).toString() );
            }
            final Blob blob = bb.build();

            final File finalTempFile = tempFile;
            final ListenableFuture<String> future =
                    blobStore.putBlob( bucketName, blob, PutOptions.Builder.multipart() );

            Runnable listener = new Runnable() {
                @Override
                public void run() {
                    try {
                        String eTag = future.get();
                        fileMetadata.put( AssetUtils.E_TAG, eTag );
                        EntityManager em = emf.getEntityManager( appId );
                        em.update( entity );
                        finalTempFile.delete();
                    }
                    catch ( Exception e ) {
                        LOG.error( "error uploading", e );
                    }
                    if ( finalTempFile != null && finalTempFile.exists() ) {
                        finalTempFile.delete();
                    }
                }
            };
            future.addListener( listener, executor );
        }
    }


    @Override
    public InputStream read( UUID appId, Entity entity, long offset, long length ) throws IOException {
        BlobStore blobStore = getContext().getBlobStore();
        Blob blob;
        if ( offset == 0 && length == FIVE_MB ) {
            blob = blobStore.getBlob( bucketName, AssetUtils.buildAssetKey( appId, entity ) );
        }
        else {
            GetOptions options = GetOptions.Builder.range( offset, length );
            blob = blobStore.getBlob( bucketName, AssetUtils.buildAssetKey( appId, entity ), options );
        }
        if ( blob == null || blob.getPayload() == null ) {
            return null;
        }
        return blob.getPayload().getInput();
    }


    @Override
    public InputStream read( UUID appId, Entity entity ) throws IOException {
        return read( appId, entity, 0, FIVE_MB );
    }


    @Override
    public void delete( UUID appId, Entity entity ) {
        BlobStore blobStore = getContext().getBlobStore();
        blobStore.removeBlob( bucketName, AssetUtils.buildAssetKey( appId, entity ) );
    }
}

