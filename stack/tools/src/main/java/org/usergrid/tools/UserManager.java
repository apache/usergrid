package org.usergrid.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.User;

import static org.usergrid.persistence.Schema.DICTIONARY_CREDENTIALS;
import static org.usergrid.utils.JsonUtils.mapToFormattedJsonString;

/**
 * @author zznate
 */
public class UserManager extends ToolBase {

  @Override
  public Options createOptions() {
    Options options = super.createOptions();
    options.addOption("u","username",true,"The username to lookup");
    options.addOption("p","password",true,"The password to set for the user");
    return options;
  }

  @Override
  public void runTool(CommandLine line) throws Exception {
    startSpring();
    String userName = line.getOptionValue("u");

    UserInfo userInfo = managementService.findAdminUser(userName);
    if ( userInfo == null ) {
      logger.info("user {} not found", userName);
      return;
    }

    logger.info(mapToFormattedJsonString(userInfo));

 
 
    if ( line.hasOption("p") ) {
      String password = line.getOptionValue("p");
      managementService.setAdminUserPassword(userInfo.getUuid(), password);
      logger.info("new password match?: " + managementService.verifyAdminUserPassword(userInfo.getUuid(), password));
    }



  }
}
