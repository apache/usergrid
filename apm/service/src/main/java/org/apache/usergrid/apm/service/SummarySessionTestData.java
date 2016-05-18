package org.apache.usergrid.apm.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;


import com.ideawheel.common.model.ApigeeMobileAPMConstants;

import com.ideawheel.portal.model.SessionChartCriteria;
import com.ideawheel.portal.model.SummarySessionMetrics;

public class SummarySessionTestData
{
   
   static String[] networkCarriers = {"att", "verizon", "sprint", "tmobile"};
   static Long[] appId = {1l, 456l, 3l};
   static String[] networkType = {"2g", "3g", "wifi"};
   static long baseLatency = 50L;
   static String[] uids = {"alan", "prabhat", "ideawheel", "cloud", "amazon", "google"};
   static String[] deviceIds = {"apple-iphone", "motorola-droid", "apple-ipad", "cloud-pc", "amazon-kindle", "google-nexus"};
   static String[] telephoneDeviceId = {"alan-iphone", "prabhat-droid", "ideawheel-ipad", "cloud-pc", "amazon-kindle", "google-nexus"};
   static String[] deviceModel = {"apple-iphone4g","apple-ipad","samsung-galaxytab","htc-thunder","google-nexusone"};
   //static String[] deviceModels = {"apple","samsung","htc","google"};
   static Long[] sessionLengths = {20L,100L, 150L,200L,250L};
   
   public static enum OutputType{DB,CSV};
   

   static Random generator = new Random();
   
   public static void populateSummarySessionDataForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId ) {
	   Vector<Device> v = new Vector<Device> ();
		Device d = new Device ();
		d.setUniqueDeviceId (UUID.randomUUID().toString());
		d.setModel (deviceModel[generator.nextInt(deviceModel.length)]);
		//d.setPlatform (devicePlatform[generator.nextInt(devicePlatform.length)]);		
		d.setNetworkCarrier (networkCarriers[generator.nextInt(networkCarriers.length)]);
		d.setNetworkType ( networkType[generator.nextInt(networkType.length)]);
		v.add(d);
		SummarySessionTestData.populateSummarySessionDataForMinutes(numMin, startTime, numDevices, givenAppId, v);
   }
   
   public static void populateSummarySessionDataForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId, Vector<Device> devices )
   {
      

      SessionDBService service = ServiceFactory.getSessionDBService();
      
      List<SessionChartCriteria> cqs = ServiceFactory.getSessionChartCriteriaService().getDefaultChartCriteriaForApp(givenAppId);
       
      
      int cqSize = cqs.size();
      
      for (int j = 0; j < numMin ; j++)
      {
         startTime.add(Calendar.MINUTE, 1);
         for (int l = 0; l < numDevices; l++) {//put 10 random beans
            startTime.set(Calendar.SECOND, generator.nextInt(60));   
            
            Calendar endTime =Calendar.getInstance();
            endTime.setTime(startTime.getTime());
            endTime.add(Calendar.SECOND, generator.nextInt(60));

            Date start = startTime.getTime();
            Date end = endTime.getTime();

            SummarySessionMetrics bean = new SummarySessionMetrics();

            int index = generator.nextInt(uids.length);

            if (givenAppId == 0)
            {
               bean.setAppId(appId[generator.nextInt(3)]);
            }
            else
            {
               bean.setAppId(givenAppId);
            }
            
            bean.setChartCriteriaId(cqs.get(generator.nextInt(cqSize)).getId());
            bean.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
            bean.setDeviceId(deviceIds[index]);
            bean.setNetworkCarrier(networkCarriers[generator.nextInt(4)]);
            bean.setNetworkType(networkType[generator.nextInt(3)]);
            bean.setDeviceModel(deviceModel[generator.nextInt(5)]);  
            
            bean.setSessionLength(new Long (generator.nextInt(100)));
            bean.setUserActivityCount(new Long (generator.nextInt(4))); 
            bean.setSessionStartTime(startTime.getTime());
            bean.setSessionEndTime(endTime.getTime());
            
            service.saveSummarySessionMetrics(bean);
         }
      }
   }
   
   

   /**
    * @param args
    */
   public static void main(String[] args)
   {
      // TODO Auto-generated method stub

   }

}
