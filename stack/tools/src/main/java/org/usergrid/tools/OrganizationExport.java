package org.usergrid.tools;

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

  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
 

  @Override
  public void runTool(CommandLine line) throws Exception {
    startSpring();

    setVerbose(line);

    prepareBaseOutputFileName(line);


    outputDir = createOutputParentDir();

    logger.info("Export directory: {}", outputDir.getAbsolutePath());

    CSVWriter writer = new CSVWriter(new FileWriter(outputDir.getAbsolutePath()+"/admins.csv"), ',');
    
    writer.writeNext(new String[]{"Organization Name", "Admin Name", "Admin Email", "Admin Created Date"});
    
    List<OrganizationInfo> organizations = managementService.getOrganizations(null, 1000000);
    
    for (OrganizationInfo organization : organizations) {
      logger.info("Org Name: {} key: {}", organization.getName(), organization.getUuid());
   
      for(UserInfo user: managementService.getAdminUsersForOrganization(organization.getUuid())){
        
        Entity admin = managementService.getAdminUserEntityByUuid(user.getUuid());
        
        Long createdDate = (Long) admin.getProperties().get("created");
        
       
        writer.writeNext(new String[]{organization.getName(), user.getName(), user.getEmail(), createdDate == null? "Unknown" : sdf.format(new Date(createdDate)) });
      }

    }
    
    logger.info("Completed export");
    
    writer.flush();
    writer.close();
  }

  @Override
  public Options createOptions() {
    Options options = super.createOptions();
   

    return options;
  }

 
 

}
