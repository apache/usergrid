package org.apache.usergrid.management.cassandra;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.management.ExportInfo;
import org.apache.usergrid.management.export.S3Export;


/**
 * Streams / reads the information written from the export service to a file named "test.json"
 */
public class MockS3ExportImpl implements S3Export {
    public static String filename;
    @Override
    public void copyToS3( final InputStream inputStream, final ExportInfo exportInfo, String filename ) {
        Logger logger = LoggerFactory.getLogger( MockS3ExportImpl.class );
        int read = 0;
        byte[] bytes = new byte[1024];
        OutputStream outputStream = null;
        //FileInputStream fis = new PrintWriter( inputStream );

        try {
            outputStream = new FileOutputStream( new File( getFilename() ) );
        }
        catch ( FileNotFoundException e ) {
            e.printStackTrace();
        }


        try {
            while ( ( read = ( inputStream.read( bytes ) ) ) != -1 ) {
                outputStream.write( bytes, 0, read );
            }
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFilename () {
        return filename;
    }

    @Override
    public void setFilename (String givenName) { filename = givenName; }
}
