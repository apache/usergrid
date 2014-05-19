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
using Newtonsoft.Json;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.IntegrationTests
{
    public class MyUsergridUser : UsergridUser
    {
        [JsonProperty("city")]
        public string City { get; set; }

        [JsonProperty("password")]
        public string Password { get; set; }
    }

    [TestFixture]
    public class UserManagementTests : BaseTest
    {
        private IClient _client;
        [SetUp]
        public void Setup()
        {
            _client = InitializeClientAndLogin(AuthType.Organization);
            DeleteUserIfExists(_client, "user1");
        }

        [Test]
        public void ShouldChangePassword()
        {
            MyUsergridUser user = new MyUsergridUser { UserName = "user1", Password = "user1", Email = "user1@gmail.com", City = "city1" };
            _client.CreateUser(user);

            user = _client.GetUser<MyUsergridUser>("user1");
            Assert.IsNotNull(user);

            _client.Login("user1", "user1", AuthType.User);

            _client.ChangePassword("user1", "user1", "user1-2");

            _client.Login("user1", "user1-2", AuthType.User);
        }

        [Test]
        public void ShouldCreateUser()
        {
            MyUsergridUser user = new MyUsergridUser { UserName = "user1", Password = "user1", Email = "user1@gmail.com", City = "city1" };
            _client.CreateUser(user);
            user = _client.GetUser<MyUsergridUser>("user1");

            Assert.IsNotNull(user);
            Assert.AreEqual("user1", user.UserName);
            Assert.AreEqual("user1@gmail.com", user.Email);
            Assert.AreEqual("city1", user.City);
            Assert.IsNull(user.Password);

            _client.DeleteUser("user1");
        }

        [Test]
        public void ShouldUpdateUser()
        {
            var user = new MyUsergridUser {UserName = "user1", Password = "user1", Email = "user1@gmail.com", City = "city1"};
                _client.CreateUser(user);
                user = _client.GetUser<MyUsergridUser>("user1");

            user.Email = "user-2@gmail.com";
            user.City = "city1-2";
            user.Password = "user1-2";
            _client.UpdateUser(user);

            user = _client.GetUser<MyUsergridUser>("user1");
            Assert.IsNotNull(user);
            Assert.AreEqual("user1", user.UserName);
            Assert.AreEqual("user-2@gmail.com", user.Email);
            Assert.AreEqual("city1-2", user.City);
            Assert.IsNull(user.Password);

            _client.DeleteUser("user1");
        }
    }
}
