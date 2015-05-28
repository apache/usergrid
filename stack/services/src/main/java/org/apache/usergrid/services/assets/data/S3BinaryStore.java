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


import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.inject.Module;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.utils.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.options.GetOptions;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S3BinaryStore implements BinaryStore {

    private static final Iterable<? extends Module> MODULES = ImmutableSet
            .of( new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(), new NettyPayloadModule() );

    private static final Logger LOG = LoggerFactory.getLogger( S3BinaryStore.class );
    private static final long FIVE_MB = ( FileUtils.ONE_MB * 5 );
    private static String WORKERS_PROP_NAME = "usergrid.binary.upload-workers";

    private BlobStoreContext context;
    private String accessId;
    private String secretKey;
    private String bucketName;
    private ExecutorService executorService;

    @Autowired
    private Properties properties;

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

        // write up to 5mb of data to an byte array

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long written = IOUtils.copyLarge( inputStream, baos, 0, FIVE_MB );
        byte[] data = baos.toByteArray();

        if ( written < FIVE_MB ) { // total smaller than 5mb

            final String uploadFileName = AssetUtils.buildAssetKey( appId, entity );
            final String mimeType = AssetMimeHandler.get().getMimeType( entity, data );

            final Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( entity );
            fileMetadata.put( AssetUtils.LAST_MODIFIED, System.currentTimeMillis() );

            BlobStore blobStore = getContext().getBlobStore();

            // need this for JClouds 1.7.x:
//            BlobBuilder.PayloadBlobBuilder bb =  blobStore.blobBuilder( uploadFileName )
//                .payload( data ).calculateMD5().contentType( mimeType );

            // need this for JClouds 1.8.x:
            BlobBuilder.PayloadBlobBuilder bb = blobStore.blobBuilder(uploadFileName)
                .payload( data )
                .contentMD5( Hashing.md5().newHasher().putBytes( data ).hash() )
                .contentType( mimeType );

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

            ExecutorService executors = getExecutorService();

            executors.submit( new UploadWorker( appId, entity, inputStream, data, written ) );
        }
    }

    private ExecutorService getExecutorService() {

        if ( executorService == null ) {
            synchronized (this) {

                int workers = 40;
                String workersString = properties.getProperty( WORKERS_PROP_NAME, "40");

                if ( StringUtils.isNumeric( workersString ) ) {
                    workers = Integer.parseInt( workersString );
                } else if ( !StringUtils.isEmpty( workersString )) {
                    LOG.error("Ignoring invalid setting for {}", WORKERS_PROP_NAME);
                }
                executorService = Executors.newFixedThreadPool( workers );
            }
        }

        return executorService;
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

    class UploadWorker implements Callable<Void> {

        private UUID appId;
        private Entity entity;
        private InputStream inputStream;
        private byte[] data;
        private long written;


        public UploadWorker( UUID appId, Entity entity, InputStream is, byte[] data, long written ) {
            this.appId = appId;
            this.entity = entity;
            this.inputStream = is;
            this.data = data;
            this.written = written;
        }

        @Override
        public Void call() {

            LOG.debug( "Writing temp file for S3 upload" );

            // determine max size file allowed, default to 50mb
            long maxSizeBytes = 50 * FileUtils.ONE_MB;
            String maxSizeMbString = properties.getProperty( "usergrid.binary.max-size-mb", "50" );
            if (StringUtils.isNumeric( maxSizeMbString )) {
                maxSizeBytes = Long.parseLong( maxSizeMbString ) * FileUtils.ONE_MB;
            }

            // always allow files up to 5mb
            if (maxSizeBytes < 5 * FileUtils.ONE_MB ) {
                maxSizeBytes = 5 * FileUtils.ONE_MB;
            }

            // write temporary file, slightly larger than our size limit
            OutputStream os = null;
            File tempFile;
            try {
                tempFile = File.createTempFile( entity.getUuid().toString(), "tmp" );
                tempFile.deleteOnExit();
                os = new BufferedOutputStream( new FileOutputStream( tempFile.getAbsolutePath() ) );
                os.write( data );
                written += data.length;
                written += IOUtils.copyLarge( inputStream, os, 0, maxSizeBytes + 1 );

                LOG.debug("Write temp file {} length {}", tempFile.getName(), written);

            } catch ( IOException e ) {
                throw new RuntimeException( "Error creating temp file", e );

            } finally {
                if ( os != null ) {
                    try {
                        os.flush();
                    } catch (IOException e) {
                        LOG.error( "Error flushing data to temporary upload file", e );
                    }
                    IOUtils.closeQuietly( os );
                }
            }

            // if tempFile is too large, delete it, add error to entity file metadata and abort

            Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( entity );

            if ( tempFile.length() > maxSizeBytes ) {
                LOG.debug("File too large. Temp file size (bytes) = {}, " +
                          "Max file size (bytes) = {} ", tempFile.length(), maxSizeBytes);
                try {
                    EntityManager em = emf.getEntityManager( appId );
                    fileMetadata.put( "error", "Asset size " + tempFile.length()
                                    + " is larger than max size of " + maxSizeBytes );
                    em.update( entity );
                    tempFile.delete();

                } catch ( Exception e ) {
                    LOG.error( "Error updating entity with error message", e);
                }
                return null;
            }

            String uploadFileName = AssetUtils.buildAssetKey( appId, entity );
            String mimeType = AssetMimeHandler.get().getMimeType( entity, data );

            try {  // start the upload

                LOG.debug( "S3 upload thread started" );

                BlobStore blobStore = getContext().getBlobStore();

                // need this for JClouds 1.7.x:
//                BlobBuilder.PayloadBlobBuilder bb =  blobStore.blobBuilder( uploadFileName )
//                    .payload( tempFile ).calculateMD5().contentType( mimeType );

                // need this for JClouds 1.8.x:
                BlobBuilder.PayloadBlobBuilder bb = blobStore.blobBuilder( uploadFileName )
                    .payload( tempFile )
                    .contentMD5( Files.hash( tempFile, Hashing.md5() ) )
                    .contentType( mimeType );

                if ( fileMetadata.get( AssetUtils.CONTENT_DISPOSITION ) != null ) {
                    bb.contentDisposition( fileMetadata.get( AssetUtils.CONTENT_DISPOSITION ).toString() );
                }
                final Blob blob = bb.build();

                String md5sum = Hex.encodeHexString( blob.getMetadata().getContentMetadata().getContentMD5() );
                fileMetadata.put( AssetUtils.CHECKSUM, md5sum );

                LOG.debug( "S3 upload starting" );

                String eTag = blobStore.putBlob( bucketName, blob );

                LOG.debug( "S3 upload complete eTag=" + eTag);

                // update entity with eTag
                EntityManager em = emf.getEntityManager( appId );
                fileMetadata.put( AssetUtils.LAST_MODIFIED, System.currentTimeMillis() );
                fileMetadata.put( AssetUtils.CONTENT_LENGTH, written );
                fileMetadata.put( AssetUtils.E_TAG, eTag );
                em.update( entity );
            }
            catch ( Exception e ) {
                LOG.error( "error uploading", e );
            }

            if ( tempFile != null && tempFile.exists() ) {
                tempFile.delete();
            }

            return null;
        }
    }
}

