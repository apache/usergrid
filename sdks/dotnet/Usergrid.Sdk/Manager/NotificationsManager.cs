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
using RestSharp;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Manager
{
    internal class NotificationsManager : ManagerBase, INotificationsManager
    {
        public NotificationsManager(IUsergridRequest request) : base(request)
        {
        }

        public void CreateNotifierForApple(string notifierName, string environment, string p12CertificatePath)
        {
            var formParameters = new Dictionary<string, object>
                {
                    {"name", notifierName},
                    {"provider", "apple"},
                    {"environment", environment}
                };

            var fileParameters = new Dictionary<string, string>
                {
                    {"p12Certificate", p12CertificatePath}
                };


            IRestResponse response = Request.ExecuteMultipartFormDataRequest("/notifiers", Method.POST, formParameters, fileParameters);
            ValidateResponse(response);
        }

        public void CreateNotifierForAndroid(string notifierName, string apiKey)
        {
            IRestResponse response = Request.ExecuteJsonRequest("/notifiers", Method.POST, new AndroidNotifierPayload {ApiKey = apiKey, Name = notifierName});
            ValidateResponse(response);
        }

        public void PublishNotification(IEnumerable<Notification> notifications, INotificationRecipients recipients, NotificationSchedulerSettings schedulerSettings = null)
        {
            var payload = new NotificationPayload();
            foreach (Notification notification in notifications)
            {
                payload.Payloads.Add(notification.NotifierIdentifier, notification.GetPayload());
            }

            if (schedulerSettings != null)
            {
                if (schedulerSettings.DeliverAt != DateTime.MinValue)
                    payload.DeliverAt = schedulerSettings.DeliverAt.ToUnixTime();
                if (schedulerSettings.ExpireAt != DateTime.MinValue)
                    payload.ExpireAt = schedulerSettings.ExpireAt.ToUnixTime();
            }

            string query = recipients.BuildQuery();

            IRestResponse response = Request.ExecuteJsonRequest(query, Method.POST, payload);
            ValidateResponse(response);
        }
    }
}
