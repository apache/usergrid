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

namespace Usergrid.Sdk.Tests.ClientTests
{
    [TestFixture]
    public class GroupTests
    {
        [SetUp]
        public void Setup()
        {
            _entityManager = Substitute.For<IEntityManager>();
            _request = Substitute.For<IUsergridRequest>();
            _client = new Client(null, null, request: _request) {EntityManager = _entityManager};
        }

        private IEntityManager _entityManager;
        private IClient _client;
        private IUsergridRequest _request;

        [Test]
        public void AddUserToGroupShouldDelegateToEntityManagerrWithCorrectConnectionAndIdentifiers()
        {
            _client.AddUserToGroup("groupIdentifier", "userIdentifier");

            _entityManager.Received(1).CreateEntity<object>("/groups/groupIdentifier/users/userIdentifier", null);
        }

        [Test]
        public void CreateGroupShouldDelegateToEntityManagerWithCorrectCollectionNameAndUser()
        {
            var group = new UsergridGroup();

            _client.CreateGroup(group);

            _entityManager.Received(1).CreateEntity("groups", group);
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void CreateGroupShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.CreateEntity("groups", Arg.Any<UsergridGroup>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.CreateGroup<UsergridGroup>(null);
        }

        [Test]
        public void DeleteGroupShouldDelegateToEntityManagerrWithCorrectCollectionNameAndIdentfier()
        {
            _client.DeleteGroup("groupPath");

            _entityManager.Received(1).DeleteEntity("groups", "groupPath");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void DeleteGroupShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.DeleteEntity("groups", Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.DeleteGroup(null);
        }

        [Test]
        public void DeleteUserFromGroupShouldDelegateToEntityManagerrWithCorrectConnectionAndIdentifier()
        {
            _client.DeleteUserFromGroup("groupIdentifier", "userIdentifier");

            _entityManager.Received(1).DeleteEntity("/groups/groupIdentifier/users", "userIdentifier");
        }

        [Test]
        public void GetGroupShouldDelegateToEntityManagerWithCorrectCollectionNameAndIdentifier()
        {
            _client.GetGroup<UsergridGroup>("identifier");

            _entityManager.Received(1).GetEntity<UsergridGroup>("groups", "identifier");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void GetGroupShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.GetEntity<UsergridGroup>("groups", Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.GetGroup<UsergridGroup>(null);
        }

        [Test]
        public void GetGroupShouldReturnNullForUnexistingGroup()
        {
            _entityManager.GetEntity<UsergridUser>("groups", "identifier").Returns(x => null);

            var usergridGroup = _client.GetGroup<UsergridGroup>("identifier");

            Assert.IsNull(usergridGroup);
        }

        [Test]
        public void GetGroupShouldReturnUsergridGroup()
        {
            var usergridGroup = new UsergridGroup();
            _entityManager.GetEntity<UsergridGroup>("groups", "identifier").Returns(x => usergridGroup);

            var returnedGroup = _client.GetGroup<UsergridGroup>("identifier");

            Assert.AreEqual(usergridGroup, returnedGroup);
        }

        [Test]
        public void UpdateGroupShouldDelegateToEntityManagerrWithCorrectCollectionNameAndGroupPathAsTheIdentifier()
        {
            var group = new UsergridGroup {Path = "groupPath"};

            _client.UpdateGroup(group);

            _entityManager.Received(1).UpdateEntity("groups", group.Path, group);
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void UpdateGroupShouldPassOnTheException()
        {
            var group = new UsergridGroup {Path = "groupPath"};

            _entityManager
                .When(m => m.UpdateEntity("groups", group.Path, group))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.UpdateGroup(group);
        }

        [Test]
        public void GetAllUsersInGroupShouldGetAllUsersInGroup()
        {
            var expectedUserList = new List<UsergridUser>() {new UsergridUser() {UserName = "userName", Name = "user1"}};
            var responseContent = new UsergridGetResponse<UsergridUser>() {Entities = expectedUserList};
            var restResponse = Helpers.SetUpRestResponseWithContent<UsergridGetResponse<UsergridUser>>(HttpStatusCode.OK, responseContent);
            
            _request.ExecuteJsonRequest("/groups/groupName/users", Method.GET).Returns(restResponse);

            var returnedUsers = _client.GetAllUsersInGroup<UsergridUser>("groupName");

            _request.Received(1).ExecuteJsonRequest("/groups/groupName/users", Method.GET);
            Assert.AreEqual(1, returnedUsers.Count);
            Assert.AreEqual("userName", returnedUsers[0].UserName);
            Assert.AreEqual("user1", returnedUsers[0].Name);
        }

        [Test]
        public void GetAllUsersInGroupWillThrowWhenBadRequest()
        {
            UsergridError error = new UsergridError() {Description = "exception description", Error = "error code"};
            var restResponse = Helpers.SetUpRestResponseWithContent<UsergridError>(HttpStatusCode.BadRequest, error);
            
            _request.ExecuteJsonRequest("/groups/groupName/users", Method.GET).Returns(restResponse);

            try
            {
                _client.GetAllUsersInGroup<UsergridUser>("groupName");
                Assert.Fail("Was expecting Usergrid exception to be thrown.");
            }
            catch (UsergridException e)
            {
                Assert.AreEqual(error.Description, e.Message);
                Assert.AreEqual(error.Error, e.ErrorCode);
            }
        }
    }
}
