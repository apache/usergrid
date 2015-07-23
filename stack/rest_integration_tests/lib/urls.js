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
var config = require("../config");
module.exports = {
    appUrl: function () {
        return config.serverUrl + config.orgName + "/" + config.appName + "/";
    },
    managementUrl: function () {
        return config.serverUrl + "management/";
    },
    appendAccessToken: function(url,tokenData){
        if(tokenData == null){
            return url;
        }
        var token = tokenData.access_token || tokenData;
        return url + (url.indexOf("?") >= 0 ? "&" : "?" ) + "access_token="+token;
    },
    appendOrgCredentials: function(url, clientId, clientSecret){
        clientId = clientId || config.org.clientId;
        clientSecret = clientSecret || config.org.clientSecret;
        return url + (url.indexOf("?") >= 0 ? "&" : "?" ) + "client_id="+clientId+"&client_secret="+clientSecret;
    }
};
