package org.usergrid.services.assets.data;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;


/** Detect the mime type of an Asset */
public final class AssetMimeHandler {
    private static final Logger LOG = LoggerFactory.getLogger( AssetMimeHandler.class );

    private Detector detector;


    AssetMimeHandler() {
        detector = new DefaultDetector();
    }


    private static AssetMimeHandler INSTANCE;


    public static AssetMimeHandler get() {
        if ( INSTANCE == null ) {
            INSTANCE = new AssetMimeHandler();
        }
        return INSTANCE;
    }


    /**
     * Get the Mime type of an Asset based on its type. If the Asset already has the "content-type" property set, we
     * return that. Otherwise the Apache Tika library is used to do file type detection.
     *
     * @return A string representation of the content type suitable for use in an HTTP header. Eg. "image/jpeg" for a
     * jpeg
     *         image.
     */
    public <T> String getMimeType( Entity entity, T type ) {

        Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( entity );
        if ( fileMetadata.get( AssetUtils.CONTENT_TYPE ) != null ) {
            return ( String ) fileMetadata.get( AssetUtils.CONTENT_TYPE );
        }

        Metadata metadata = new Metadata();
        MediaType mediaType = MediaType.OCTET_STREAM;
        try {
            if ( type instanceof byte[] ) {

                ByteArrayInputStream bais = new ByteArrayInputStream( ( byte[] ) type );
                mediaType = detector.detect( bais, metadata );
            }
            else if ( type instanceof File ) {

                InputStream fis = new BufferedInputStream( new FileInputStream( ( File ) type ) );
                try {
                    mediaType = detector.detect( fis, metadata );
                }
                finally {
                    fis.close();
                }
            }
            else {
                return mediaType.toString();
            }

            fileMetadata.put( AssetUtils.CONTENT_TYPE, mediaType.toString() );
        }
        catch ( IOException e ) {
            LOG.error( "error detecting mime type", e );
        }

        return mediaType.toString();
    }
}
