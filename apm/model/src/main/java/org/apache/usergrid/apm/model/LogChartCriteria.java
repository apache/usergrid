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
package org.apache.usergrid.apm.model;

import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "LOG_CHART_CRITERIA")
@org.hibernate.annotations.Table(appliesTo="LOG_CHART_CRITERIA",
indexes = {
@Index(name="LogChartCriteriaByApp", columnNames={"appId"} )
} )
public class LogChartCriteria extends ChartCriteria
{

   private Long assertCount;
   private Long errorCount;
   private Long warnCount;
   private Long infoCount;
   private Long debugCount;
   private Long verboseCount;
   private Long errorAndAboveCount;
   private Long eventCount;
   
   private boolean showAssertCount;
   private boolean showErrorCount;
   private boolean showWarnCount;
   private boolean showInfoCount;
   private boolean showDebugCount;
   private boolean showVerboseCount;
   private boolean showErrorAndAboveCount;
   private boolean showEventCount;
   private boolean showAll;
   
   public enum LOG_LEVEL_STRING {VERBOSE,DEBUG,INFO,WARN,ERROR,ASSERT};

   public Long getAssertCount()
   {
      return assertCount;
   }

   public void setAssertCount(Long assertCount)
   {
      this.assertCount = assertCount;
   }

   public Long getErrorCount()
   {
      return errorCount;
   }

   public void setErrorCount(Long errorCount)
   {
      this.errorCount = errorCount;
   }

   public Long getWarnCount()
   {
      return warnCount;
   }

   public void setWarnCount(Long warnCount)
   {
      this.warnCount = warnCount;
   }

   public Long getInfoCount()
   {
      return infoCount;
   }

   public void setInfoCount(Long infoCount)
   {
      this.infoCount = infoCount;
   }

   public Long getDebugCount()
   {
      return debugCount;
   }

   public void setDebugCount(Long debugCount)
   {
      this.debugCount = debugCount;
   }

   public Long getVerboseCount()
   {
      return verboseCount;
   }

   public void setVerboseCount(Long verboseCount)
   {
      this.verboseCount = verboseCount;
   }

   public Long getErrorAndAboveCount()
   {
      return errorAndAboveCount;
   }

   public void setErrorAndAboveCount(Long errorAndAboveCount)
   {
      this.errorAndAboveCount = errorAndAboveCount;
   }

   public Long getEventCount()
   {
      return eventCount;
   }
   
  
   public void setEventCount(Long eventCount)
   {
      this.eventCount = eventCount;
   }

   public boolean isShowAssertCount()
   {
      return showAssertCount;
   }

   public void setShowAssertCount(boolean showAssertCount)
   {
      this.showAssertCount = showAssertCount;
   }

   public boolean isShowErrorCount()
   {
      return showErrorCount;
   }

   public void setShowErrorCount(boolean showErrorCount)
   {
      this.showErrorCount = showErrorCount;
   }

   public boolean isShowWarnCount()
   {
      return showWarnCount;
   }

   public void setShowWarnCount(boolean showWarnCount)
   {
      this.showWarnCount = showWarnCount;
   }

   public boolean isShowInfoCount()
   {
      return showInfoCount;
   }

   public void setShowInfoCount(boolean showInfoCount)
   {
      this.showInfoCount = showInfoCount;
   }

   public boolean isShowDebugCount()
   {
      return showDebugCount;
   }

   public void setShowDebugCount(boolean showDebugCount)
   {
      this.showDebugCount = showDebugCount;
   }

   public boolean isShowVerboseCount()
   {
      return showVerboseCount;
   }

   public void setShowVerboseCount(boolean showVerboseCount)
   {
      this.showVerboseCount = showVerboseCount;
   }

   public boolean isShowErrorAndAboveCount()
   {
      return showErrorAndAboveCount;
   }

   public void setShowErrorAndAboveCount(boolean showErrorAndAboveCount)
   {
      this.showErrorAndAboveCount = showErrorAndAboveCount;
   }

   public boolean isShowEvent()
   {
      return showEventCount;
   }

   public void setShowEvent(boolean showEvent)
   {
      this.showEventCount = showEvent;
   }

   public boolean isShowAll()
   {
      return showAll;
   }

   public void setShowAll(boolean showAll)
   {
      if(showAll) {
         showAssertCount = true;
         showErrorCount = true;
         showWarnCount = true;
         showInfoCount = true;
         showDebugCount = true;
         showVerboseCount = true;
         showErrorAndAboveCount = true;
         showEventCount = true;         
      }
      this.showAll = showAll;
   }   
   
   

}
