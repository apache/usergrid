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
package org.apache.usergrid.apm.service;

import org.apache.usergrid.apm.model.DeviceModel;
import org.apache.usergrid.apm.model.DevicePlatform;
import org.apache.usergrid.apm.model.NetworkCarrier;
import org.apache.usergrid.apm.model.NetworkSpeed;

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
