package org.usergrid.services.assets.data;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import org.usergrid.persistence.Entity;

import org.apache.commons.io.FileUtils;


/** A binary store implementation using the local file system */
public class LocalFileBinaryStore implements BinaryStore {

    private String reposLocation = FileUtils.getTempDirectoryPath();


    /** Control where to store the file repository. In the system's temp dir by default. */
    public void setReposLocation( String reposLocation ) {
        this.reposLocation = reposLocation;
    }


    public String getReposLocation() {
        return reposLocation;
    }


    /**
     * Common method of contructing the file object based on the configured repos and {@link
     * org.usergrid.persistence.entities.Asset#getPath()}
     */
    private File path( UUID appId, Entity entity ) {
        return new File( reposLocation, AssetUtils.buildAssetKey( appId, entity ) );
    }


    @Override
    public void write( UUID appId, Entity entity, InputStream inputStream ) throws IOException {

        File file = path( appId, entity );

        FileUtils.copyInputStreamToFile( inputStream, file );

        long size = FileUtils.sizeOf( file );

        Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( entity );
        fileMetadata.put( AssetUtils.CONTENT_LENGTH, size );
        fileMetadata.put( AssetUtils.LAST_MODIFIED, System.currentTimeMillis() );

        // if we were successful, write the mime type
        if ( file.exists() ) {
            AssetMimeHandler.get().getMimeType( entity, file );
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
     * Deletes the asset if it is a file. Does nothing if {@link org.usergrid.persistence.entities.Asset#getPath()}
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
