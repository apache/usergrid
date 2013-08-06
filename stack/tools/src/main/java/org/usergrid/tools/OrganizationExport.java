package org.usergrid.tools;

import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.collect.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.time.DateUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.*;
import org.usergrid.persistence.Results.Level;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.Group;
import org.usergrid.tools.bean.*;
import org.usergrid.utils.TimeUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Tools class which dumps metrics for tracking Usergrid developer adoption and
 * high-level application usage.
 * 
 * Can be called thusly: mvn exec:java
 * -Dexec.mainClass="org.usergrid.tools.Command"
 * -Dexec.args="Metrics -host localhost -outputDir ./output"
 * 
 * @author zznate
 */
public class OrganizationExport extends ExportingToolBase {

  /**
   * 
   */
  private static final String QUERY_ARG = "query";
  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

  @Override
  public void runTool(CommandLine line) throws Exception {
    startSpring();

    setVerbose(line);

    prepareBaseOutputFileName(line);

    outputDir = createOutputParentDir();

    String queryString = line.getOptionValue(QUERY_ARG);

    Query query = Query.fromQL(queryString);

    logger.info("Export directory: {}", outputDir.getAbsolutePath());

    CSVWriter writer = new CSVWriter(new FileWriter(outputDir.getAbsolutePath() + "/admins.csv"), ',');

    writer.writeNext(new String[] { "Organization Name", "Admin Name", "Admin Email", "Admin Created Date" });

    Results organizations = null;

    do {
      
      organizations = getOrganizations(query);
      
      for (Entity organization : organizations.getEntities()) {
        String orgName = organization.getProperty("path").toString();
        
        logger.info("Org Name: {} key: {}", orgName, organization.getUuid());

        for (UserInfo user : managementService.getAdminUsersForOrganization(organization.getUuid())) {

          Entity admin = managementService.getAdminUserEntityByUuid(user.getUuid());

          Long createdDate = (Long) admin.getProperties().get("created");

          writer.writeNext(new String[] { orgName, user.getName(), user.getEmail(),
              createdDate == null ? "Unknown" : sdf.format(new Date(createdDate)) });
        }
      }

      query.setCursor(organizations.getCursor());
      
    } while (organizations != null && organizations.hasCursor());

    logger.info("Completed export");

    writer.flush();
    writer.close();
  }

  @Override
  public Options createOptions() {
    Options options = super.createOptions();

    @SuppressWarnings("static-access")
    Option queryOption = OptionBuilder.withArgName(QUERY_ARG).hasArg().isRequired(true)
        .withDescription("Query to execute when searching for organizations").create(QUERY_ARG);
    options.addOption(queryOption);

    return options;
  }

  private Results getOrganizations(Query query) throws Exception {

    EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
    return em.searchCollection(em.getApplicationRef(), "groups", query);
  }

}
