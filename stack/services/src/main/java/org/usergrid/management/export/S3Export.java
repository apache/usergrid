package org.usergrid.management.export;


import java.io.InputStream;

import org.usergrid.management.ExportInfo;


/**
 *
 *
 */
public interface S3Export {
    void copyToS3( InputStream inputStream, ExportInfo exportInfo, String filename );

}
