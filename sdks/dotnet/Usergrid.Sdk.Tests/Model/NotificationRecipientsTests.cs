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
using NSubstitute.Core;
using NUnit.Framework;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests.Model
{
    [TestFixture]
    public class NotificationRecipientsTests
    {
        [Test]
        public void NotificationRecipientsBuildsQuery1()
        {
            var query = new NotificationRecipients()
                .AddDeviceWithName("d")
                .AddGroupWithPath("p")
                .AddUserWithName("u")
                .BuildQuery();

            Assert.AreEqual("/groups/p/users/u/devices/d/notifications", query);
        }

        [Test]
        public void NotificationRecipientsBuildsQuery2()
        {
            var query = new NotificationRecipients()
                .AddDeviceWithName("d")
                .AddGroupWithPath("p")
                .AddUserWithUuid("u")
                .BuildQuery();

            Assert.AreEqual("/groups/p/users/u/devices/d/notifications", query);
        }

        [Test]
        public void NotificationRecipientsBuildsQuery3()
        {
            var query = new NotificationRecipients()
                .AddDeviceWithQuery("d")
                .AddGroupWithQuery("p")
                .AddUserWithQuery("u")
                .BuildQuery();

            Assert.AreEqual("/groups;ql=p/users;ql=u/devices;ql=d/notifications", query);
        }
        [Test]
        public void NotificationRecipientsBuildsQuery4()
        {
            var query = new NotificationRecipients()
                .AddDeviceWithQuery(string.Empty)
                .AddGroupWithQuery(string.Empty)
                .AddUserWithQuery(string.Empty)
                .BuildQuery();

            Assert.AreEqual("/groups;ql=/users;ql=/devices;ql=/notifications", query);
        }

        [Test]
        [ExpectedException(typeof(ArgumentException), ExpectedMessage = "User name and uuid can not be added at the same time.")]
        public void UserNameAndUuidCannotBeAddedAtTheSameTime()
        {
            new NotificationRecipients()
                .AddUserWithName("u")
                .AddUserWithUuid("i");
        }

        [Test]
        [ExpectedException(typeof(ArgumentException), ExpectedMessage = "User name and uuid can not be added at the same time.")]
        public void UserUuidAndNameCannotBeAddedAtTheSameTime()
        {
            new NotificationRecipients()
                .AddUserWithUuid("i")
                .AddUserWithName("u");
        }

        [Test]
        [ExpectedException(typeof(ArgumentException), ExpectedMessage = "User query can not be added together with user name or uuid.")]
        public void UserUuidAndQueryCannotBeAddedAtTheSameTime()
        {
            new NotificationRecipients()
                .AddUserWithUuid("i")
                .AddUserWithQuery("u");
        }

        [Test]
        [ExpectedException(typeof(ArgumentException), ExpectedMessage = "User query can not be added together with user name or uuid.")]
        public void UserNameAndQueryCannotBeAddedAtTheSameTime()
        {
            new NotificationRecipients()
                .AddUserWithName("i")
                .AddUserWithQuery("u");
        }

        [Test]
        [ExpectedException(typeof(ArgumentException), ExpectedMessage = "Group path and query can not be added at the same time.")]
        public void GroupPathAndQueryCannotBeAddedAtTheSameTime()
        {
            new NotificationRecipients()
                .AddGroupWithPath("u")
                .AddGroupWithQuery("i");
        }

        [Test]
        [ExpectedException(typeof(ArgumentException), ExpectedMessage = "Group path and query can not be added at the same time.")]
        public void GroupQueryAndPathCannotBeAddedAtTheSameTime()
        {
            new NotificationRecipients()
                .AddGroupWithQuery("i")
                .AddGroupWithPath("u");
        }

        [Test]
        [ExpectedException(typeof(ArgumentException), ExpectedMessage = "Device name and query can not be added at the same time.")]
        public void DeviceQueryAndNameCannotBeAddedAtTheSameTime()
        {
            new NotificationRecipients()
                .AddDeviceWithQuery("i")
                .AddDeviceWithName("u");
        }

        [Test]
        [ExpectedException(typeof(ArgumentException), ExpectedMessage = "Device name and query can not be added at the same time.")]
        public void DeviceNameAndQueryCannotBeAddedAtTheSameTime()
        {
            new NotificationRecipients()
                .AddDeviceWithName("i")
                .AddDeviceWithQuery("u");
        }

    }
}
