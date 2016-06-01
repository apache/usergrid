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

import org.apache.usergrid.apm.service.LogTestData;
import org.apache.usergrid.apm.service.NetworkTestData;
import org.apache.usergrid.apm.service.SessionTestData;
import org.apache.usergrid.apm.service.SummarySessionTestData;

public class PopulateCompleteTestData
{

   /**
    * @param args
    */
   public static void main(String[] args)
   {

      //while (true)  {
      System.out.println ("#################################");
      System.out.println ("#                                        #");
      System.out.println ("#  Going to populate session  test data. #");
      System.out.println ("#      InstaOps Inc                       #");
      System.out.println ("#                               #");
      System.out.println ("#################################");

      System.out.println();
      System.out.println();

      
     // ApplicationService appService = ServiceFactory.getApplicationService();
      //App app1 = appService.getApplication(2L);

      System.out.println ("Populating Metrics for app1");
      long dataInsertStartTime = System.currentTimeMillis();
      Calendar original = Calendar.getInstance();
      original.add(Calendar.HOUR_OF_DAY, -1);


      //Data for next 3 hours       
     /* SessionTestData.populateSessionDataForMinutes(100, (Calendar) original.clone(),5, app1.getInstaOpsApplicationId());
      SummarySessionTestData.populateSummarySessionDataForMinutes(100, (Calendar) original.clone(),5, app1.getInstaOpsApplicationId());
      LogTestData.populateLogForMinutes(100, (Calendar) original.clone(),5, app1.getInstaOpsApplicationId());
      LogTestData.populateRawLogs(100, (Calendar) original.clone(),5, app1.getInstaOpsApplicationId());    
      NetworkTestData.populateCompactNetworkMetricsForMinutes(100, (Calendar) original.clone(),5, app1.getInstaOpsApplicationId());
      NetworkTestData.populateDataForMinutes(100, (Calendar) original.clone(),5, app1.getInstaOpsApplicationId());
      */
      

      //Data for previous 2 weeks and next 2 weeks       
     // Calendar prev14 = (Calendar) original.clone();
      //prev14.add(Calendar.HOUR, -1*24*7*2);
      //int numMinutes = 60*24*7*8;
      Calendar prev14 = (Calendar) original.clone();
      int numMinutes = 300;
      long appidtemp = 2L;
      SessionTestData.populateSessionDataForMinutes(numMinutes, (Calendar) prev14.clone(),5, appidtemp);
      SummarySessionTestData.populateSummarySessionDataForMinutes(numMinutes, (Calendar) prev14.clone(),5, appidtemp);
      LogTestData.populateLogForMinutes(numMinutes, (Calendar) prev14.clone(),5, appidtemp);
      LogTestData.populateRawLogs(numMinutes, (Calendar) prev14.clone(),5, appidtemp);
      NetworkTestData.populateCompactNetworkMetricsForMinutes (numMinutes, (Calendar) prev14.clone(),5, appidtemp);
      NetworkTestData.populateDataForMinutes(numMinutes, (Calendar) prev14.clone(),5, appidtemp);

//      //Data for comparing with yesterday
//      Calendar yesterday = (Calendar) original.clone();
//      yesterday.add(Calendar.DAY_OF_YEAR, -2);      
//      SessionTestData.populateSessionDataForMinutes(60*24, (Calendar) yesterday.clone(),5, app1.getInstaOpsApplicationId());
//      SummarySessionTestData.populateSummarySessionDataForMinutes(60*24, (Calendar) yesterday.clone(),5, app1.getInstaOpsApplicationId());
//      LogTestData.populateLogForMinutes(60*24, (Calendar) yesterday.clone(),5, app1.getInstaOpsApplicationId());
//      NetworkTestData.populateCompactNetworkMetricsForMinutes (60*24, (Calendar) yesterday.clone(),5, app1.getInstaOpsApplicationId());
//      NetworkTestData.populateDataForMinutes(60*24, (Calendar) yesterday.clone(),5, app1.getInstaOpsApplicationId());
//
//      //Data for comparing with last week
//      Calendar lastWeek = (Calendar) original.clone();
//      lastWeek.add(Calendar.DAY_OF_YEAR, -14);
//      SessionTestData.populateSessionDataForMinutes(1, (Calendar) lastWeek.clone(),5, app1.getInstaOpsApplicationId());
//      SummarySessionTestData.populateSummarySessionDataForMinutes(100, (Calendar) lastWeek.clone(),5, app1.getInstaOpsApplicationId());
//      LogTestData.populateLogForMinutes(100, (Calendar) lastWeek.clone(),5, app1.getInstaOpsApplicationId());
//      NetworkTestData.populateCompactNetworkMetricsForMinutes (100, (Calendar) lastWeek.clone(),5, app1.getInstaOpsApplicationId());
//      NetworkTestData.populateDataForMinutes(100, (Calendar) lastWeek.clone(),5, app1.getInstaOpsApplicationId());
      
      long dataInsertEndTime = System.currentTimeMillis();
      System.out.println("Time taken to insert data " + (dataInsertEndTime - dataInsertStartTime));

      System.out.println ("##############################");
      System.out.println ("#                            #");
      System.out.println ("#   And, WE ARE DONE         #");
      System.out.println ("#         ENJOY              #");
      System.out.println ("#                            #");
      System.out.println ("##############################");
   }

}
