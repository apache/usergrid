package org.apache.usergrid.perftest.amazon;

import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.usergrid.perftest.settings.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;


/**
 * Information about the Amazon EC2 Instance we run on.
 */
public class Ec2Metadata extends Properties implements Props {
    private static final Logger LOG = LoggerFactory.getLogger( Ec2Metadata.class );

    private static final String EC2METADATA_PROCESS = "/usr/bin/ec2metadata";
    private static final String PUBLIC_HOSTNAME_KEY = "public-hostname";
    private static final String LOCAL_HOSTNAME_KEY = "local-hostname";
    private static final String PUBLIC_IPV4_KEY = "public-ipv4";
    private static final String LOCAL_IPV4_KEY = "local-ipv4";
    private static final String INSTANCE_URL = "http://169.254.169.254/latest/meta-data";

    private boolean isRunningOnEc2 = false;


    public Ec2Metadata() {
        super();

        if ( new File( EC2METADATA_PROCESS ).exists() ) {
            isRunningOnEc2 = true;
        }
        else {
            isRunningOnEc2 = false;
            return;
        }

        try {
            File file = File.createTempFile( "ec2metadata", "out" );
            ProcessBuilder pb = new ProcessBuilder( EC2METADATA_PROCESS );
            pb.redirectOutput( file );
            Process process = pb.start();

            try {
                process.waitFor();
            }
            catch ( InterruptedException e ) {
                LOG.error( "Interrupted while waiting for process {}", EC2METADATA_PROCESS, e );
            }

            load( new FileInputStream( file ) );

            if ( LOG.isDebugEnabled() )
            {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                store( out, null );
                LOG.debug( "Contents of Ec2Metadata =\n{}", new String( out.toByteArray() ) );
            }
        }
        catch ( IOException e ) {
            LOG.error( "Failed to execute process {}", EC2METADATA_PROCESS, e );
        }
    }


    public Ec2Metadata( InputStream in ) throws IOException {
        super();
        load( in );
    }


    @SuppressWarnings( "UnusedDeclaration" )
    @JsonProperty
    public String getAmiId() {
        return getProperty( "ami-id" );
    }


    @SuppressWarnings( "UnusedDeclaration" )
    @JsonProperty
    public String getAvailabilityZone() {
        return getProperty( "availability-zone" );
    }


    @SuppressWarnings( "UnusedDeclaration" )
    @JsonProperty
    public String getInstanceId() {
        return getProperty( "instance-id" );
    }


    @SuppressWarnings( "UnusedDeclaration" )
    @JsonProperty
    public String getInstanceType() {
        return getProperty( "instance-type" );
    }


    @SuppressWarnings( "UnusedDeclaration" )
    @JsonProperty
    public String getLocalHostname() {
        return getProperty( "local-hostname" );
    }


    @SuppressWarnings( "UnusedDeclaration" )
    @JsonProperty
    public String getLocalIpv4() {
        return getProperty( "local-ipv4" );
    }


    @SuppressWarnings( "UnusedDeclaration" )
    @JsonProperty
    public String getPublicIpv4() {
        return getProperty( "public-ipv4" );
    }


    @JsonProperty
    public String getPublicHostname() {
        return getProperty( PUBLIC_HOSTNAME_KEY );
    }


    @JsonProperty
    public int getServerPort() {
        String portStr = getProperty( SERVER_PORT_KEY, "8080" );
        return new Integer( portStr );
    }


    /**
     * Gets the properties listing as an input stream.
     * @return the properties listing as an input stream
     * @throws IOException there are io failures
     */
    public InputStream getPropertiesAsStream() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        store( bytes, null );
        bytes.flush();
        return new StringInputStream( bytes.toString() );
    }


    /**
     * Get whether or not we are running on EC2.
     *
     * @return true if running on an EC2 instance, false otherwise
     */
    @SuppressWarnings( "UnusedDeclaration" )
    public boolean runningOnEc2() {
        return isRunningOnEc2;
    }


    public String getUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append( "http://" ).append( getPublicHostname() ).append( ':' ).append( getServerPort() );
        return sb.toString();
    }


    /**
     * Pulls a number of properties from the EC2 instance metadata. Is fail fast
     * to make sure we don't make a number of calls and block. This is the old way
     * to do it which can block for a bit when not running on EC2.
     *
     * @return true if we succeed in getting all the information we need
     */
    @SuppressWarnings( "UnusedDeclaration" )
    private boolean populateEc2Metadata()
    {
        try {
            setProperty( PUBLIC_HOSTNAME_KEY, extractEc2Metadata( PUBLIC_HOSTNAME_KEY ) );
            setProperty( LOCAL_HOSTNAME_KEY, extractEc2Metadata( LOCAL_HOSTNAME_KEY ) );
            setProperty( PUBLIC_IPV4_KEY, extractEc2Metadata( PUBLIC_IPV4_KEY ) );
            setProperty( LOCAL_IPV4_KEY, extractEc2Metadata( LOCAL_IPV4_KEY ) );
            return true;
        }
        catch ( IOException ioe ) {
            return false;
        }
    }


    /**
     * Extracts instance meta data from the AmazonS3Service#INSTANCE_URL URL. This is
     * the old way to do it that takes a bit of time timing out when not on EC2.
     *
     * @param data the information to get
     * @return the value of the information associated with the EC2 instance
     * @throws IOException if there's a failure accessing the EC2 instance
     */
    private String extractEc2Metadata( String data ) throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append( INSTANCE_URL ).append( '/' ).append( data );

        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet( url.toString() );

        try {
            HttpResponse response = client.execute( request );
            int statusCode = response.getStatusLine().getStatusCode();

            if ( statusCode == 200 ) {
                int initCapacity = ( int ) response.getEntity().getContentLength() + 1;
                ByteArrayOutputStream out = new ByteArrayOutputStream( initCapacity );
                response.getEntity().writeTo( out );
                out.flush();
                out.close();
                return out.toString();
            }

            throw new IOException( "Got bad status " + statusCode + " for URL " + url );
        }
        catch ( IOException ioe ) {
            LOG.error( "Failed to extract {} from {}", data, url );
            throw ioe;
        }
    }
}
