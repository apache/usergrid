// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

using System;
using System.Collections.Generic;
using System.Dynamic;
using RestSharp;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Model
{

    public class AppleNotification : Notification
    {
        public AppleNotification(string notifierIdentifier, string message, string sound = null)
            : base(notifierIdentifier, message)
        {
            Sound = sound;
        }

        public string Sound { get; set; }

        internal override object GetPayload()
        {

            if (Sound != null)
            {
                return new {aps = new {alert = Message, sound = Sound}};
            }
            else
            {
                return Message;
            }
        }
    }

}
