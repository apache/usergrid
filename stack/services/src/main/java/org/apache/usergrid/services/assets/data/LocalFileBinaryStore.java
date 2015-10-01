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


import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;


/** A binary store implementation using the local file system */
public class LocalFileBinaryStore implements BinaryStore {

    private Logger LOG = LoggerFactory.getLogger( LocalFileBinaryStore.class );

    private String reposLocation = FileUtils.getTempDirectoryPath();

    private static final long FIVE_MB = ( FileUtils.ONE_MB * 5 );

    @Autowired
    private Properties properties;

    @Autowired
    private EntityManagerFactory emf;

    /** Control where to store the file repository. In the system's temp dir by default. */
    public void setReposLocation( String reposLocation ) {
        this.reposLocation = reposLocation;
    }


    public String getReposLocation() {
        return reposLocation;
    }


    /**
     * Common method of contructing the file object based on the configured repos and {@link
     * org.apache.usergrid.persistence.entities.Asset#getPath()}
     */
    private File path( UUID appId, Entity entity ) {
        return new File( reposLocation, AssetUtils.buildAssetKey( appId, entity ) );
    }


    @Override
    public void write( UUID appId, Entity entity, InputStream inputStream ) throws IOException {

        File file = path( appId, entity );

        FileUtils.copyInputStreamToFile( inputStream, file );

        long size = FileUtils.sizeOf( file );

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

        EntityManager em = emf.getEntityManager( appId );
        Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( entity );

        if ( size > maxSizeBytes ) {
            try {
                fileMetadata.put( "error", "Asset size " + size
                        + " is larger than max size of " + maxSizeBytes );
                em.update( entity );

            } catch ( Exception e ) {
                LOG.error( "Error updating entity with error message", e);
            }
            return;
        }

        fileMetadata.put( AssetUtils.CONTENT_LENGTH, size );
        fileMetadata.put( AssetUtils.LAST_MODIFIED, System.currentTimeMillis() );
        fileMetadata.put( AssetUtils.E_TAG, RandomStringUtils.randomAlphanumeric( 10 ) );

        // if we were successful, write the mime type
        if ( file.exists() ) {
            fileMetadata.put( AssetUtils.CONTENT_TYPE , AssetMimeHandler.get().getMimeType( entity, file ));
        }

        try {
            em.update( entity );
        } catch (Exception e) {
            throw new IOException("Unable to update entity filedata", e);
        }
    }


    @Override
    public InputStream read( UUID appId, Entity entity ) throws IOException {
        return read( appId, entity, 0, FileUtils.ONE_MB * 5 );
    }


    @Override
    public InputStream read( UUID appId, Entity entity, long offset, long length ) throws IOException {
        return new BufferedInputStream( FileUtils.openInputStream( path( appId, entity ) ) );
    }


    /**
     * Deletes the asset if it is a file. Does nothing if {@link org.apache.usergrid.persistence.entities.Asset#getPath()}
     * represents a directory.
     */
    @Override
    public void delete( UUID appId, Entity entity ) {
        File file = path( appId, entity );
        if ( file.exists() && !file.isDirectory() ) {
            FileUtils.deleteQuietly( file );
        }
    }
}
