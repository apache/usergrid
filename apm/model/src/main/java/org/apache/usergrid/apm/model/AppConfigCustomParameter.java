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

import java.io.Serializable;


/**
 * 
 * @author prabhat
 *
 */

public class AppConfigCustomParameter  implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8467213354781894334L;

	private String tag;
	
	private String paramKey;
	
	private String paramValue;
	
	public AppConfigCustomParameter() {
		// TODO Auto-generated constructor stub
	}
	
	public AppConfigCustomParameter (String tag, String key, String value) {
		this.paramKey = key;
		this.paramValue = value;
		this.tag = tag;		
	}
	
	 public String getTag()
	   {
	      return tag;
	   }

	   public void setTag(String tag)
	   {
	      this.tag = tag;
	   }

	   
	   public String getParamKey()
	   {
	      return paramKey;
	   }

	   public void setParamKey(String key)
	   {
	      this.paramKey = key;
	   }

	   public String getParamValue()
	   {
	      return paramValue;
	   }

	   public void setParamValue(String value)
	   {
	      this.paramValue = value;
	   }
		


	@Override
	public String toString() {
		return "Custom config - tag : " + tag + " key " + paramKey + " value " + paramValue;
	}

	
  	

}
