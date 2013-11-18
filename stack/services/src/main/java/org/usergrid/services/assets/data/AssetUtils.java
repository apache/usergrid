package org.usergrid.services.assets.data;


import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.utils.StringUtils;

import org.apache.commons.lang.time.DateUtils;

import com.google.common.base.Preconditions;


public class AssetUtils {
    private static Logger LOG = LoggerFactory.getLogger( AssetUtils.class );

    public static final String FILE_METADATA = "file-metadata";
    public static final String CONTENT_TYPE = "content-type";
    public static final String CONTENT_LENGTH = "content-length";
    public static final String CONTENT_DISPOSITION = "content-disposition";
    public static final String E_TAG = "etag";
    public static final String CHECKSUM = "checksum";
    public static final String LAST_MODIFIED = "last-modified";


    /** Returns the key for the bucket in the following form: [appId]/[{@link org.usergrid.persistence.entities
     * .Asset#getPath()} */
    public static String buildAssetKey( UUID appId, Entity entity ) {
        Preconditions.checkArgument( entity.getUuid() != null, "The asset provided to buildAssetKey had a null UUID" );
        Preconditions.checkArgument( appId != null, "The appId provided to buildAssetKey was null" );
        return appId.toString().concat( "/" ).concat( entity.getUuid().toString() );
    }


    /** Attempt to parse the Date from a Date-based header, primarily If-Modified-Since */
    public static Date fromIfModifiedSince( String headerValue ) {
        Date moded = null;
        if ( !StringUtils.isEmpty( headerValue ) ) {
            try {
                moded = DateUtils.parseDate( headerValue, DEFAULT_PATTERNS );
            }
            catch ( ParseException pe ) {
                LOG.error( "Could not parse date format from If-Modified-Since header: " + headerValue );
            }
        }
        return moded;
    }


    /** side-effect: sets file-metadata on the entity if not present */
    public static Map<String, Object> getFileMetadata( Entity entity ) {
        Map<String, Object> metadata = ( Map<String, Object> ) entity.getProperty( AssetUtils.FILE_METADATA );
        if ( metadata == null ) {
            metadata = new HashMap<String, Object>();
            entity.setProperty( AssetUtils.FILE_METADATA, metadata );
            addLegacyMetadata( entity, metadata );
        }
        // must always have a last modified. if not in the metadata, grab the entity's
        if ( metadata.get( LAST_MODIFIED ) == null ) {
            metadata.put( LAST_MODIFIED, entity.getModified() );
        }
        return metadata;
    }


    /** @deprecated for legacy use */
    private static void addLegacyMetadata( Entity entity, Map<String, Object> metadata ) {
        if ( entity.getProperty( CONTENT_TYPE ) != null ) {
            metadata.put( CONTENT_TYPE, entity.getProperty( CONTENT_TYPE ) );
        }
        if ( entity.getProperty( CONTENT_LENGTH ) != null ) {
            metadata.put( CONTENT_LENGTH, entity.getProperty( CONTENT_LENGTH ) );
        }
        if ( entity.getProperty( CONTENT_DISPOSITION ) != null ) {
            metadata.put( CONTENT_DISPOSITION, entity.getProperty( CONTENT_DISPOSITION ) );
        }
        if ( entity.getProperty( E_TAG ) != null ) {
            metadata.put( E_TAG, entity.getProperty( E_TAG ) );
        }
        if ( entity.getProperty( CHECKSUM ) != null ) {
            metadata.put( CHECKSUM, entity.getProperty( CHECKSUM ) );
        }
    }

    /***
     * The following yanked from org.apache.http.impl.cookie.DateUtils, Apache 2.0 License
     ***/

    /** Date format pattern used to parse HTTP date headers in RFC 1123 format. */
    public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /** Date format pattern used to parse HTTP date headers in RFC 1036 format. */
    public static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

    /** Date format pattern used to parse HTTP date headers in ANSI C <code>asctime()</code> format. */
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

    public static final String[] DEFAULT_PATTERNS = new String[] {
            PATTERN_RFC1036, PATTERN_RFC1123, PATTERN_ASCTIME
    };
}
