package org.usergrid.management.export;


import org.usergrid.management.ExportInfo;


/**
 *
 *
 */
public interface S3Export {
    void copyToS3( String fileName, ExportInfo exportInfo );

}
