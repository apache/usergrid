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
    public class FeedTests
    {
        #region Setup/Teardown

        [SetUp]
        public void Setup()
        {
            _entityManager = Substitute.For<IEntityManager>();
            _client = new Client(null, null) {EntityManager = _entityManager};
        }

        #endregion

        private IEntityManager _entityManager;
        private IClient _client;

        [Test]
        public void GetGroupFeedShouldDelegateToEntityManagerWithCorrectEndpoint()
        {
            var usergridActivities = new UsergridCollection<UsergridActivity>();
            _entityManager.GetEntities<UsergridActivity>("/groups/groupIdentifier/feed").Returns(usergridActivities);

            UsergridCollection<UsergridActivity> returnedActivities = _client.GetGroupFeed<UsergridActivity>("groupIdentifier");

            _entityManager.Received(1).GetEntities<UsergridActivity>("/groups/groupIdentifier/feed");
            Assert.AreEqual(usergridActivities, returnedActivities);
        }

        [Test]
        public void GetUserFeedShouldDelegateToEntityManagerWithCorrectEndpoint()
        {
            var usergridActivities = new UsergridCollection<UsergridActivity>();
            _entityManager.GetEntities<UsergridActivity>("/users/userIdentifier/feed").Returns(usergridActivities);

            UsergridCollection<UsergridActivity> returnedActivities = _client.GetUserFeed<UsergridActivity>("userIdentifier");

            _entityManager.Received(1).GetEntities<UsergridActivity>("/users/userIdentifier/feed");
            Assert.AreEqual(usergridActivities, returnedActivities);
        }
    }
}
