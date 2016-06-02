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
package org.apache.usergrid.apm.service.charts.service;

import java.io.Serializable;
import java.util.Date;

public abstract class RawData implements Serializable, Comparable<RawData> { 

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   Date timeStamp;

   public void setTimeStamp (Date timeStamp) {
      this.timeStamp = timeStamp;
   }
   public Date getTimeStamp () {
      return timeStamp;
   }

   public int compareTo(RawData o) {
      if (this.timeStamp.before(o.getTimeStamp()))
         return 1; //descending order by timestamp
      else if (this.timeStamp.after(o.getTimeStamp()))
         return -1;
      else return 0;

   }

}
