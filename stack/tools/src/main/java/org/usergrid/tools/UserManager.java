package org.usergrid.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.usergrid.management.UserInfo;

/**
 * @author zznate
 */
public class UserManager extends ToolBase {

  @Override
  public Options createOptions() {
    Options options = super.createOptions();
    options.addOption("u","username",true,"The username to lookup");
    options.addOption("p","password",true,"The password to use for verification");
    return options;
  }

  @Override
  public void runTool(CommandLine line) throws Exception {
    startSpring();
    String userName = line.getOptionValue("u");
    String password = line.getOptionValue("p");

    UserInfo userInfo = managementService.findAdminUser(userName);
    if ( userInfo == null ) {
      System.out.println("user not found");
    } else {
      System.out.println("password match" + managementService.verifyAdminUserPassword(userInfo.getUuid(), password));
    }
  }
}
