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
using System.Net;
using NSubstitute;
using NUnit.Framework;
using RestSharp;
using Usergrid.Sdk.Manager;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Tests
{
    [TestFixture]
    public class NotificationsManagerTests
    {
        private IUsergridRequest _request;
        private NotificationsManager _notificationsManager;

        [SetUp]
        public void Setup()
        {
            _request = Substitute.For<IUsergridRequest>();
            _notificationsManager = new NotificationsManager(_request);
        }


        [Test]
        public void CreateNotifierForAppleExecutesMultipartFormDataRequestWithCorrectParameters()
        {
            IRestResponse restResponse = Helpers.SetUpRestResponse(HttpStatusCode.OK);
            _request.ExecuteMultipartFormDataRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<IDictionary<string, object>>(), Arg.Any<IDictionary<string, string>>())
                .Returns(restResponse);

            _notificationsManager.CreateNotifierForApple("notifierName", "development", @"C:\filePath");

            _request.Received(1).ExecuteMultipartFormDataRequest(
                "/notifiers",
                Method.POST,
                Arg.Is<IDictionary<string, object>>(d => (string) d["name"] == "notifierName" && (string) d["provider"] == "apple" && (string) d["environment"] == "development"),
                Arg.Is<IDictionary<string, string>>(d => d["p12Certificate"] == @"C:\filePath"));
        }

        [Test]
        public void CreateNotifierForAndroidExecutesMultipartFormDataRequestWithCorrectParameters()
        {
            IRestResponse restResponse = Helpers.SetUpRestResponse(HttpStatusCode.OK);
            _request.ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponse);

            _notificationsManager.CreateNotifierForAndroid("notifierName", "apiKey");

            _request.Received(1).ExecuteJsonRequest(
                "/notifiers",
                Method.POST,
                Arg.Is<AndroidNotifierPayload>(p => p.ApiKey == "apiKey" && p.Name == "notifierName" && p.Provider == "google"));
        }

        [Test]
        public void PublishNotificationPostsByBuildingQueryAndPayload()
        {
            IRestResponse restResponse = Helpers.SetUpRestResponse(HttpStatusCode.OK);
            _request.ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponse);

            var recipients = Substitute.For<INotificationRecipients>();
            recipients.BuildQuery().Returns("query");

            var appleNotification = new AppleNotification("appleNotifierName", "appleTestMessge", "chime");
            var googleNotification = new AndroidNotification("googleNotifierName", "androidTestMessage");

            var deliverAt = DateTime.Now.AddDays(1);
            var expireAt = DateTime.Now.AddDays(2);

            var schedulerSettings = new NotificationSchedulerSettings {DeliverAt = deliverAt, ExpireAt = expireAt};

            //call PublishNotification
            _notificationsManager.PublishNotification(new Notification[] {appleNotification, googleNotification}, recipients, schedulerSettings);

            //assert
            recipients.Received(1).BuildQuery();

            Func<NotificationPayload, bool> validatePayload = p =>
                                                                  {
                                                                      bool isValid = true;
                                                                      isValid &= p.DeliverAt == deliverAt.ToUnixTime();
                                                                      isValid &= p.ExpireAt == expireAt.ToUnixTime();

                                                                      var applePayload = p.Payloads["appleNotifierName"].GetReflectedProperty("aps");
                                                                      isValid &= (string) applePayload.GetReflectedProperty("alert") == "appleTestMessge";
                                                                      isValid &= (string) applePayload.GetReflectedProperty("sound") == "chime";

                                                                      var googlePayload = p.Payloads["googleNotifierName"].GetReflectedProperty("data");
                                                                      isValid &= (string) googlePayload == "androidTestMessage";

                                                                      return isValid;
                                                                  };
            _request.Received(1).ExecuteJsonRequest("query", Method.POST,
                                                    Arg.Is<NotificationPayload>(
                                                        p => validatePayload(p)));
        }
    }
}
