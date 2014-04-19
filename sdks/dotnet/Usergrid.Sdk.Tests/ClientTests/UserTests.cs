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

using NSubstitute;
using NUnit.Framework;
using Usergrid.Sdk.Manager;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests.ClientTests
{
    [TestFixture]
    public class UserTests
    {
        #region Setup/Teardown

        [SetUp]
        public void Setup()
        {
            _entityManager = Substitute.For<IEntityManager>();
            _authenticationManager = Substitute.For<IAuthenticationManager>();
            _client = new Client(null, null) {EntityManager = _entityManager, AuthenticationManager = _authenticationManager};
        }

        #endregion

        private IEntityManager _entityManager;
        private IAuthenticationManager _authenticationManager;
        private IClient _client;

        [Test]
        public void ChangePasswordShouldDelegateToAuthenticationManager()
        {
            _client.ChangePassword("userName", "oldPassword", "newPassword");

            _authenticationManager.Received(1).ChangePassword("userName", "oldPassword", "newPassword");
        }

        [Test]
        public void CreateUserShouldDelegateToEntityManagerWithCorrectCollectionNameAndUser()
        {
            var user = new UsergridUser();

            _client.CreateUser(user);

            _entityManager.Received(1).CreateEntity("users", user);
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void CreateUserShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.CreateEntity("users", Arg.Any<UsergridUser>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.CreateUser<UsergridUser>(null);
        }

        [Test]
        public void DeleteUserShouldDelegateToEntityManagerrWithCorrectCollectionNameAndIdentfier()
        {
            _client.DeleteUser("userName");

            _entityManager.Received(1).DeleteEntity("users", "userName");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void DeleteUserShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.DeleteEntity("users", Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.DeleteUser(null);
        }

        [Test]
        public void GetUserShouldDelegateToEntityManagerWithCorrectCollectionNameAndIdentifier()
        {
            _client.GetUser<UsergridUser>("identifier");

            _entityManager.Received(1).GetEntity<UsergridUser>("users", "identifier");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void GetUserShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.GetEntity<UsergridUser>("users", Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.GetUser<UsergridUser>(null);
        }

        [Test]
        public void GetUserShouldReturnNullForUnexistingUser()
        {
            _entityManager.GetEntity<UsergridUser>("users", "identifier").Returns((x) => null);

            var usergridUser = _client.GetUser<UsergridUser>("identifier");

            Assert.IsNull(usergridUser);
        }

        [Test]
        public void GetUserShouldReturnUsergridUser()
        {
            var usergridUser = new UsergridUser();
            _entityManager.GetEntity<UsergridUser>("users", "identifier").Returns((x) => usergridUser);

            var returnedUser = _client.GetUser<UsergridUser>("identifier");

            Assert.AreEqual(usergridUser, returnedUser);
        }

        [Test]
        public void UpdateUserShouldDelegateToEntityManagerrWithCorrectCollectionNameAndUserNameAsTheIdentifier()
        {
            var user = new UsergridUser {UserName = "userName"};

            _client.UpdateUser(user);

            _entityManager.Received(1).UpdateEntity("users", user.UserName, user);
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void UpdateUserShouldPassOnTheException()
        {
            var user = new UsergridUser {UserName = "userName"};

            _entityManager
                .When(m => m.UpdateEntity("users", user.UserName, user))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.UpdateUser(user);
        }
    }
}
