/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.locking;

import java.util.UUID;

/**
 * Helper class that contains the logic to build a lock path
 * @author eanuff
 */
public class LockPathBuilder {

	private static final String SLASH = "/";

	/**
	 * Build a string path for this lock
	 * @param applicationId
	 * @param path
	 * @return
	 */
	public static String buildPath(UUID applicationId, String... path) {
		StringBuilder builder = new StringBuilder();
		builder.append(SLASH);
		builder.append(applicationId.toString());
		
		for(String element: path){
  		builder.append(SLASH);
  		builder.append(element);
		}
		return builder.toString();
	}
	
	/**
   * Build a string path for this lock
   * @param The binary value to append to the end of the lock path
   * @param path The values to prepend to build path
   * @return
   */
  public static String buildPath(String binaryValue, String... path) {
    
    StringBuilder builder = new StringBuilder();
    
    for(String element: path){
      builder.append(SLASH);
      builder.append(element);
    }
    
    builder.append(SLASH);
    builder.append(binaryValue);
    
    builder.deleteCharAt(0);
    
    return builder.toString();
  }
	
	

  
  


}
