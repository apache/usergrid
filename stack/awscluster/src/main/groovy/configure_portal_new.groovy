/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */


//
// Emits dash/app/config.js file 
//
def baseUrl = "http://${System.getenv().get("DNS_NAME")}.${System.getenv().get("DNS_DOMAIN")}"
config = """
var VERSION = 'R-2013-07-02-02';
var Usergrid = Usergrid || {};
Usergrid.showNotifcations = true;

// used only if hostname does not match a real server name
Usergrid.overrideUrl = '${baseUrl}';

Usergrid.settings = {
  hasMonitoring:true //change to false to remove monitoring
};
"""
println config
