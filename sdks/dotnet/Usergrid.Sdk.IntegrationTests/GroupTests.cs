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

using System.Collections.Generic;
using NUnit.Framework;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.IntegrationTests
{
    public class MyUsergridGroup : UsergridGroup
    {
        public string Description { get; set; }
    }

    [TestFixture]
    public class GroupTests : BaseTest
    {
        [Test]
        public void ShouldManageGroupLifecycle()
        {
            var client = new Client(Organization, Application, ApiUri);
            client.Login(ClientId, ClientSecret, AuthType.Organization);

            var group = client.GetGroup<MyUsergridGroup>("group1");

            if (group != null)
                client.DeleteGroup("group1");

            group = new MyUsergridGroup {Path = "group1", Title = "title1", Description = "desc1"};
            client.CreateGroup(group);
            group = client.GetGroup<MyUsergridGroup>("group1");

            Assert.IsNotNull(group);
            Assert.AreEqual("group1", group.Path);
            Assert.AreEqual("title1", group.Title);
            Assert.AreEqual("desc1", group.Description);

            group.Description = "desc2";
            group.Title = "title2";

            client.UpdateGroup(group);

            group = client.GetGroup<MyUsergridGroup>("group1");

            Assert.IsNotNull(group);
            Assert.AreEqual("group1", group.Path);
            Assert.AreEqual("title2", group.Title);
            Assert.AreEqual("desc2", group.Description);

            client.DeleteGroup("group1");

            group = client.GetGroup<MyUsergridGroup>("group1");
            Assert.IsNull(group);
        }
        
        [Test]
        public void ShouldManageUsersInGroup()
        {
            var client = new Client(Organization, Application, ApiUri);
            client.Login(ClientId, ClientSecret, AuthType.Organization);

            var user = SetupUsergridUser(client, new MyUsergridUser {UserName = "user1", Password = "user1", Email = "user1@gmail.com", City = "city1"});
            var group = SetupUsergridGroup(client, new MyUsergridGroup {Path = "group1", Title = "title1", Description = "desc1"});

            client.AddUserToGroup(group.Path, user.UserName);
            IList<UsergridUser> users = client.GetAllUsersInGroup<UsergridUser>(group.Path);
            Assert.IsNotNull(users);
            Assert.AreEqual(1, users.Count);

            client.DeleteUserFromGroup("group1", "user1");
            users = client.GetAllUsersInGroup<UsergridUser>(group.Path);
            Assert.IsNotNull(users);
            Assert.AreEqual(0, users.Count);

            client.DeleteGroup("group1");
            client.DeleteUser("user1");
        }
    }
}
