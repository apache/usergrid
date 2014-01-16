package org.usergrid.tools;


import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.usergrid.management.UserInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import au.com.bytecode.opencsv.CSVWriter;

import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;


/**
 * Tools class which dumps metrics for tracking Usergrid developer adoption and high-level application usage.
 * <p/>
 * Can be called thusly: mvn exec:java -Dexec.mainClass="org.usergrid.tools.Command" -Dexec.args="Metrics -host
 * localhost -outputDir ./output"
 *
 * @author zznate
 */
public class OrganizationExport extends ExportingToolBase {

    /**
     *
     */
    private static final String QUERY_ARG = "query";
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm" );


    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        setVerbose( line );

        prepareBaseOutputFileName( line );

        outputDir = createOutputParentDir();

        String queryString = line.getOptionValue( QUERY_ARG );

        Query query = Query.fromQL( queryString );

        logger.info( "Export directory: {}", outputDir.getAbsolutePath() );

        CSVWriter writer = new CSVWriter( new FileWriter( outputDir.getAbsolutePath() + "/admins.csv" ), ',' );

        writer.writeNext( new String[] { "Organization Name", "Organization UUID", "Admin Name", "Admin Email", "Admin UUID", "Admin Activated?", "Admin Confirmed?", "Admin Disabled?", "Admin Created Date" } );

        Results organizations = null;

        do {

            organizations = getOrganizations( query );

            for ( Entity organization : organizations.getEntities() ) {
                String orgName = organization.getProperty( "path" ).toString();

                logger.info( "Org Name: {} key: {}", orgName, organization.getUuid() );

                for ( UserInfo user : managementService.getAdminUsersForOrganization( organization.getUuid() ) ) {

                    Entity admin = managementService.getAdminUserEntityByUuid( user.getUuid() );

                    Long createdDate  = ( Long ) admin.getProperties().get( "created" );
                    Boolean activated = ( Boolean ) admin.getProperties().get( "activated" );
                    Boolean confirmed = ( Boolean ) admin.getProperties().get( "confirmed" );
                    Boolean disabled  = ( Boolean ) admin.getProperties().get( "disabled" );

                    writer.writeNext( new String[] {
                            orgName,
                            ( String ) organization.getUuid().toString(),
                            user.getName(),
                            user.getEmail(),
                            ( String ) user.getUuid().toString(),
                            activated == null ? "Unknown" : ( String ) activated.toString(),
                            confirmed == null ? "Unknown" : ( String ) confirmed.toString(),
                            disabled  == null ? "Unknown" : ( String ) disabled.toString(),
                            createdDate == null ? "Unknown" : ( String ) createdDate.toString()
                    } );
                }
            }

            query.setCursor( organizations.getCursor() );
        }
        while ( organizations != null && organizations.hasCursor() );

        logger.info( "Completed export" );

        writer.flush();
        writer.close();
    }


    @Override
    public Options createOptions() {
        Options options = super.createOptions();

        @SuppressWarnings("static-access") Option queryOption =
                OptionBuilder.withArgName( QUERY_ARG ).hasArg().isRequired( true )
                             .withDescription( "Query to execute when searching for organizations" )
                             .create( QUERY_ARG );
        options.addOption( queryOption );

        return options;
    }


    private Results getOrganizations( Query query ) throws Exception {

        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );
        return em.searchCollection( em.getApplicationRef(), "groups", query );
    }
}
