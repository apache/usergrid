package org.usergrid.tools;

import org.apache.commons.cli.CommandLine;
import org.usergrid.management.UserInfo;

/**
 * @author zznate
 */
public class UserManager extends ToolBase {

  @Override
  public void runTool(CommandLine line) throws Exception {
    String userName = line.getOptionValue("u");
    String password = line.getOptionValue("p");

    UserInfo userInfo = managementService.findAdminUser(userName);

    System.out.println("password match" + managementService.verifyAdminUserPassword(userInfo.getUuid(), password));
  }
}
