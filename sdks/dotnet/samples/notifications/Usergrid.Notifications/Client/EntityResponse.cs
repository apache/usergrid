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

using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace Usergrid.Notifications.Client
{
    public class EntityResponse : JObject
    {
        private HttpResponseMessage response;

        private EntityResponse(HttpResponseMessage response, JObject jObject)
            : base()
        {
            this.response = response;
            parseResponse(jObject);
        }

        private void parseResponse(JObject jobject)
        {
            foreach (var kv in jobject)
            {
                this[kv.Key] = kv.Value;
            }
        }

        public HttpStatusCode Status
        {
            get { return this.response.StatusCode; }
        }

        public static async Task<EntityResponse> Parse(HttpResponseMessage httpResponseMessage)
        {
            JObject jobject;
            
            using (var stream = await httpResponseMessage.Content.ReadAsStreamAsync())
            {
                using (var reader = new StreamReader(stream, Encoding.UTF8))
                {
                    jobject = JObject.Parse(reader.ReadToEnd());
                }
            }
            
            return new EntityResponse(httpResponseMessage, jobject); 
        }

        public bool StatusIsOk
        {
            get
            {
                return this.Status == HttpStatusCode.OK;
            }
        }
    }
}
