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


using NUnit.Framework;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests.Model
{
    [TestFixture]
    public class NotificationTests
    {
        [Test]
        public void AndroidNotificationShouldReturnCorrectPayload()
        {
            var notification = new AndroidNotification("notifierName", "notification message");
            var payload = notification.GetPayload();

            Assert.AreEqual("notification message", payload.GetReflectedProperty("data"));
        }

        [Test]
        public void GoogleNotificationShouldReturnCorrectPayloadWithSound()
        {
            var notification = new AppleNotification("notifierName", "notification message", "chime");
            var payload = notification.GetPayload();

            var aps = payload.GetReflectedProperty("aps");
            Assert.IsNotNull(aps);
            Assert.AreEqual("notification message", aps.GetReflectedProperty("alert"));
            Assert.AreEqual("chime", aps.GetReflectedProperty("sound"));
        }

        [Test]
        public void GoogleNotificationShouldReturnCorrectPayloadWithoutSound()
        {
            var notification = new AppleNotification("notifierName", "notification message");
            var payload = notification.GetPayload();

            Assert.AreEqual("notification message", payload);
        }
    }
}
