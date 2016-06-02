/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.usergrid.apm.util;

import java.util.Calendar;

import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.ApplicationConfigurationModel;
import org.apache.usergrid.apm.model.App;

import org.apache.usergrid.apm.service.ApplicationService;
import org.apache.usergrid.apm.service.LogTestData;
import org.apache.usergrid.apm.service.ServiceFactory;




public class PopulateLogTestData {

   /**
    * @param args
    */
   public static void main(String[] args) {		 


      //while (true)  {
      System.out.println ("#################################");
      System.out.println ("#                               #");
      System.out.println ("#  Going to populate test data.  #");
      System.out.println ("#      opsFuse Inc              #");
      System.out.println ("#                               #");
      System.out.println ("#################################");

      System.out.println();
      System.out.println();
      
      System.out.println("Adding test app logs for app1 and app2");
      ApplicationService appService = ServiceFactory.getApplicationService();

      App app1 = appService.getApplication(1L);
      App app2 = appService.getApplication(2L);
      
      if (app1 == null) {	   

         app1 = new App ();
         app1.setAppOwner("user1");
         app1.setAppName("App1");
         app1.setDefaultAppConfig(new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT));
         app1 = appService.createApplication(app1);
         System.out.println ("App added with id " + app1.getInstaOpsApplicationId() + " name " + app1.getAppName() + " owner " + app1.getAppOwner());
      }

      if (app2 == null) {
         app2 = new App ();
         app2.setAppName("App2");
         app2.setAppOwner("user2");
         app2.setDefaultAppConfig(new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT));
         app2 = appService.createApplication (app2);
         System.out.println ("App added with id " + app2.getInstaOpsApplicationId() + " name " + app2.getAppName() + " owner " + app2.getAppOwner());
   
      }    
      
      System.out.println ("Populating logs for both apps");
      Calendar startTime = Calendar.getInstance();
      startTime.add(Calendar.HOUR_OF_DAY, -1);
      LogTestData.populateLogForMinutes(200, startTime,5, app1.getInstaOpsApplicationId());
      LogTestData.populateLogForMinutes(200, startTime,5, app2.getInstaOpsApplicationId());
      
      LogTestData.populateRawLogs(10, startTime,5, app1.getInstaOpsApplicationId());
      LogTestData.populateRawLogs(10, startTime,5, app2.getInstaOpsApplicationId());



      System.out.println ("##############################");
      System.out.println ("#                            #");
      System.out.println ("#   And, WE ARE DONE         #");
      System.out.println ("#         ENJOY              #");
      System.out.println ("#                            #");
      System.out.println ("##############################");

   }	

}
