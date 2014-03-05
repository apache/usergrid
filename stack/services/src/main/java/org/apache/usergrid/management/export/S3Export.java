package org.apache.usergrid.management.export;


import java.io.InputStream;
import java.util.Map;


/**
 *
 *
 */
public interface S3Export {
    void copyToS3( InputStream inputStream, Map<String,Object> exportInfo, String filename );

    String getFilename ();

    void setFilename (String givenName);

}
