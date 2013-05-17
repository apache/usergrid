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
package org.usergrid.rest.test.resource.app.queue;

import org.usergrid.rest.test.resource.NamedResource;
import org.usergrid.rest.test.resource.ValueResource;
import org.usergrid.rest.test.resource.app.Device;

/**
 * @author tnine
 * 
 */
public class DevicesCollection extends ValueResource {

 
  public DevicesCollection(NamedResource parent) {
    super("devices", parent);
  }

 
  public Device device(String deviceName){
    return new Device(deviceName, this);
  }
}
