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
