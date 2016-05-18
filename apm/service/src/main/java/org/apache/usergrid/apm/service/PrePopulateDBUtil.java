package org.apache.usergrid.apm.service;

import com.ideawheel.portal.model.DeviceModel;
import com.ideawheel.portal.model.DevicePlatform;
import com.ideawheel.portal.model.NetworkCarrier;
import com.ideawheel.portal.model.NetworkSpeed;

public class PrePopulateDBUtil
{

   public static void prePopulateDeviceModel (String model) {

      ApplicationService service = ServiceFactory.getApplicationService();
      if (service.getDeviceModels(null).size() != 0)
         return;
      String[] models = model.split(",");
      for (String m : models) {
         service.saveDeviceModel(new DeviceModel(null,m));
      }

   }

   public static void prePopulateDevicePlatform (String platforms) {
      ApplicationService service = ServiceFactory.getApplicationService();
      if (service.getDevicePlatforms(null).size() != 0)
         return;
      String[] ps = platforms.split(",");
      for (String p : ps) {
         service.saveDevicePlatform(new DevicePlatform(null,p));
      }

   }

   public static void prePopulateNetworkSpeed (String speed) {
      ApplicationService service = ServiceFactory.getApplicationService();
      if (service.getNetworkSpeeds(null).size() != 0)
         return;
      String[] ss = speed.split(",");
      for (String s : ss) {
         service.saveNetworkSpeed(new NetworkSpeed(null,s));
      }

   }

   public static void prePopulateNetworkCarriers (String carriers) {
      ApplicationService service = ServiceFactory.getApplicationService();
      if (service.getNetworkCarriers(null).size() != 0)
         return;
      String[] ps = carriers.split(",");
      for (String p : ps) {
         service.saveNetworkCarrier(new NetworkCarrier(null,p));
      }

   }

   public static void prePopulateAdminUser(String name, String password,String email) {

   }
}
