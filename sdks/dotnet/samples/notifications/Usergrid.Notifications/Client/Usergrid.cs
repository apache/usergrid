/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

using System;
using System.Threading.Tasks;
using System.Net;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json;
using System.Net.Http;
using System.Net.Http.Headers;

namespace Usergrid.Notifications.Client
{
 
    /// <summary>
    /// Usergrid client 
    /// </summary>

    public class Usergrid : IUsergridHttpClient, IUsergridClient
    {
        private string appUrl;
        private string token;
        private HttpClient client;
        private IPushClient push;
        private string managementUrl;

        /// <summary>
        /// Constructor
        /// </summary>
        /// <param name="serverUrl"></param>
        /// <param name="org"></param>
        /// <param name="app"></param>
        /// <param name="channel"></param>
        internal Usergrid(string serverUrl, string org, string app, string userId, string password,string notifier)
        {
            string serverUrlWithSlash = serverUrl.EndsWith("/", StringComparison.CurrentCulture) ? serverUrl : serverUrl + "/";
            this.appUrl = String.Format("{0}{1}/{2}/", serverUrlWithSlash, org, app);
            this.managementUrl = serverUrlWithSlash + "management/";
            this.client = new HttpClient();
            Authenticate(userId, password, false).ContinueWith(task => {
                this.push = new PushClient(this, userId, notifier);
            });
        }

        public async Task Authenticate(string user, string password, bool isManagement)
        {
            var jsonObject = new JObject();
            jsonObject.Add("username", user);
            jsonObject.Add("password", password);
            jsonObject.Add("grant_type", "password");
           
            var response = await SendAsync(HttpMethod.Post,"token", jsonObject, isManagement);

            if (response.StatusIsOk)
            {
                this.token = response.GetValue("access_token").Value<String>();
                client.DefaultRequestHeaders.Add("Authorization", "Bearer "+ token);
            }
            else
            {
                throw new Exception("Authentication failed: "+response.ToString());
            }
        }

        private async Task<EntityResponse> GetJsonResponse(HttpResponseMessage response)
        {
            return  await EntityResponse.Parse(response);
        }

        public async Task<EntityResponse> SendAsync(HttpMethod method, string url, object obj)
        {
            return await SendAsync(method, url, obj, false);
        }

        public async Task<EntityResponse> SendAsync(HttpMethod method, string url, object obj, bool useManagementUrl)
        {
            HttpRequestMessage message = new HttpRequestMessage(method, (useManagementUrl ? this.managementUrl : this.appUrl) + url);
            if (obj != null)
            {
                message.Content = getJsonBody(obj);
            }
            message.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue( "application/json"));
            var response = await this.client.SendAsync(message);
            return await EntityResponse.Parse(response);
        }
   
        public IPushClient Push
        {
            get { return push; }
        }

        private HttpContent getJsonBody(Object jsonObject)
        {
            return new StringContent(JsonConvert.SerializeObject(jsonObject));
        }


        public Exception LastException
        {
            get;
            set;
        }
    }

}
