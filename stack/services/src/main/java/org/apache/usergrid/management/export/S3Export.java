package org.apache.usergrid.management.export;


import java.io.InputStream;

import org.apache.usergrid.management.ExportInfo;


/**
 *
 *
 */
public interface S3Export {
    void copyToS3( InputStream inputStream, ExportInfo exportInfo, String filename );

    String getFilename ();

    void setFilename (String givenName);

}
