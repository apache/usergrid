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


import com.google.api.services.storage.StorageScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.HttpTransportOptions;
import com.google.cloud.TransportOptions;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


public class GoogleBinaryStore implements BinaryStore {

    private static final Logger logger = LoggerFactory.getLogger(GoogleBinaryStore.class);
    private static final long FIVE_MB = ( FileUtils.ONE_MB * 5 );


    private EntityManagerFactory entityManagerFactory;

    private Properties properties;

    private String bucketName;

    private Storage instance = null;

    private String reposLocation = FileUtils.getTempDirectoryPath();

    public GoogleBinaryStore(Properties properties,
                             EntityManagerFactory entityManagerFactory,
                             String reposLocation) throws IOException, GeneralSecurityException {
        this.entityManagerFactory = entityManagerFactory;
        this.properties = properties;
        this.reposLocation = reposLocation;
    }

    private synchronized Storage getService() throws IOException, GeneralSecurityException {

        logger.trace("Getting Google Cloud Storage service");

        // leave this here for tests because they do things like manipulate properties dynamically to test invalid values
        this.bucketName = properties.getProperty( "usergrid.binary.bucketname" );


        if (instance == null) {

            // Google provides different authentication types which are different based on if the application is
            // running within GCE(Google Compute Engine) or GAE (Google App Engine). If Usergrid is running in
            // GCE or GAE, the SDK will automatically authenticate and get access to
            // cloud storage. Else, the full path to a credential file should be provided in the following environment variable
            //
            //     GOOGLE_APPLICATION_CREDENTIALS
            //
            // The SDK will attempt to load the credential file for a service account. See the following
            // for more info: https://developers.google.com/identity/protocols/application-default-credentials#howtheywork
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault().createScoped(StorageScopes.all());


            final TransportOptions transportOptions = HttpTransportOptions.newBuilder()
                .setConnectTimeout(30000) // in milliseconds
                .setReadTimeout(30000) // in milliseconds
                .build();

            instance = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .setTransportOptions(transportOptions)
                .build()
                .getService();
        }

        return instance;
    }


    @Override
    public void write(UUID appId, Entity entity, InputStream inputStream) throws Exception {

        getService();

        final AtomicLong writtenSize = new AtomicLong();

        final int chunkSize = 1024; // one KB

        // determine max size file allowed, default to 50mb
        long maxSizeBytes = 50 * FileUtils.ONE_MB;
        String maxSizeMbString = properties.getProperty( "usergrid.binary.max-size-mb", "50" );
        if (StringUtils.isNumeric( maxSizeMbString )) {
            maxSizeBytes = Long.parseLong( maxSizeMbString ) * FileUtils.ONE_MB;
        }

        byte[] firstData = new byte[chunkSize];
        int firstSize = inputStream.read(firstData);
        writtenSize.addAndGet(firstSize);

        // from the first sample chunk, determine the file size
        final String contentType = AssetMimeHandler.get().getMimeType(entity, firstData);

        // Convert to the Google Cloud Storage Blob
        final BlobId blobId = BlobId.of(bucketName, AssetUtils.buildAssetKey( appId, entity ));
        final BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();


        // always allow files up to 5mb
        if (maxSizeBytes < 5 * FileUtils.ONE_MB ) {
            maxSizeBytes = 5 * FileUtils.ONE_MB;
        }

        EntityManager em = entityManagerFactory.getEntityManager( appId );
        Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( entity );


        // directly upload files that are smaller than the chunk size
        if (writtenSize.get() < chunkSize ){

            // Upload to Google cloud Storage
            instance.create(blobInfo, firstData);

        }else{


            WriteChannel writer = instance.writer(blobInfo);

            // write the initial sample data used to determine file type
            writer.write(ByteBuffer.wrap(firstData, 0, firstData.length));

            // start writing remaining chunks from the stream
            byte[] buffer = new byte[chunkSize];
            int limit;
            while ((limit = inputStream.read(buffer)) >= 0) {

                writtenSize.addAndGet(limit);
                if ( writtenSize.get() > maxSizeBytes ) {
                    try {
                        fileMetadata.put( "error", "Asset size is larger than max size of " + maxSizeBytes );
                        em.update( entity );

                    } catch ( Exception e ) {
                        logger.error( "Error updating entity with error message", e);
                    }
                    return;
                }

                try {
                    writer.write(ByteBuffer.wrap(buffer, 0, limit));

                } catch (Exception ex) {
                    logger.error("Error writing chunk to Google Cloud Storage for asset ");
                }
            }

            writer.close();
        }

        fileMetadata.put( AssetUtils.CONTENT_LENGTH, writtenSize.get() );
        fileMetadata.put( AssetUtils.LAST_MODIFIED, System.currentTimeMillis() );
        fileMetadata.put( AssetUtils.E_TAG, RandomStringUtils.randomAlphanumeric( 10 ) );
        fileMetadata.put( AssetUtils.CONTENT_TYPE , contentType);

        try {
            em.update( entity );
        } catch (Exception e) {
            throw new IOException("Unable to update entity filedata", e);
        }

    }

    @Override
    public InputStream read(UUID appId, Entity entity) throws Exception {

        return read( appId, entity, 0, FIVE_MB );
    }

    @Override
    public InputStream read(UUID appId, Entity entity, long offset, long length) throws Exception {

        getService();

        final byte[] content = instance.readAllBytes(BlobId.of(bucketName, AssetUtils.buildAssetKey( appId, entity )));
        return new ByteArrayInputStream(content);
    }

    @Override
    public void delete(UUID appId, Entity entity) throws Exception {

        getService();

        final BlobId blobId = BlobId.of(bucketName, AssetUtils.buildAssetKey( appId, entity ));
        instance.delete(blobId);
    }

}
