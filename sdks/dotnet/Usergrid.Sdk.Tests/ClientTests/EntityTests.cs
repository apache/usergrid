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
    public class EntityTests
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
        public void CreateEntityShouldDelegateToEntityManagerWithCorrectParameters()
        {
            var entity = new {Name = "test"};

            _client.CreateEntity<object>("collection", entity);

            _entityManager.Received(1).CreateEntity<object>("collection", entity);
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void CreateEntityShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.CreateEntity(Arg.Any<string>(), Arg.Any<object>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.CreateEntity<object>(null, null);
        }

        [Test]
        public void CreateEntityShouldReturnUserGridEntity()
        {
            var entity = new object();
            _entityManager.CreateEntity("collection", entity).ReturnsForAnyArgs(entity);

            var returnedEntity = _client.CreateEntity("collection", entity);

            Assert.AreEqual(entity, returnedEntity);
        }

        [Test]
        public void DeleteEntityShouldDelegateToEntityManagerWithCorrectParameters()
        {
            _client.DeleteEntity("collection", "identifier");

            _entityManager.Received(1).DeleteEntity("collection", "identifier");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void DeleteEntityShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.DeleteEntity(Arg.Any<string>(), Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.DeleteEntity(null, null);
        }

        [Test]
        public void GetEntitiesShouldDefaultTheLimit()
        {
            _client.GetEntities<object>("collection");

            _entityManager.Received(1).GetEntities<object>("collection", 10);
        }

        [Test]
        public void GetEntitiesShouldDefaultTheQueryToNull()
        {
            _client.GetEntities<object>("collection", 10);

            _entityManager.Received(1).GetEntities<object>("collection", 10, null);
        }

        [Test]
        public void GetEntitiesShouldDelegateToEntityManagerWithCorrectParameters()
        {
            _client.GetEntities<object>("collection", 20, "query");

            _entityManager.Received(1).GetEntities<object>("collection", 20, "query");
        }

        [Test]
        public void GetEntityShouldDelegateToEntityManagerWithCorrectParameters()
        {
            _client.GetEntity<object>("collection", "identifier");

            _entityManager.Received(1).GetEntity<object>("collection", "identifier");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void GetEntityShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.GetEntity<object>(Arg.Any<string>(), Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.GetEntity<object>(null, null);
        }

        [Test]
        public void GetEntityShouldReturnEntityFromEntityManager()
        {
            var entity = new object();
            _entityManager.GetEntity<object>("collection", "identifier").ReturnsForAnyArgs(entity);

            object createdEntity = _client.GetEntity<object>("collection", "identifier");

            Assert.AreEqual(entity, createdEntity);
        }

        [Test]
        public void GetEntityShouldReturnNullForUnexistingEntity()
        {
            _entityManager.GetEntity<UsergridEntity>("collection", "identifier").Returns(x => null);

            var usergridEntity = _client.GetEntity<UsergridDevice>("collection", "identifier");

            Assert.IsNull(usergridEntity);
        }

        [Test]
        public void GetNextEntitiesShouldDefaultTheQueryToNull()
        {
            _client.GetNextEntities<object>("collection");

            _entityManager.Received(1).GetNextEntities<object>("collection", null);
        }

        [Test]
        public void GetNextEntitiesShouldDelegateToEntityManagerWithCorrectParameters()
        {
            _client.GetNextEntities<object>("collection", "query");

            _entityManager.Received(1).GetNextEntities<object>("collection", "query");
        }

        [Test]
        public void GetPreviousEntitiesShouldDefaultTheQueryToNull()
        {
            _client.GetPreviousEntities<object>("collection");

            _entityManager.Received(1).GetPreviousEntities<object>("collection", null);
        }

        [Test]
        public void GetPreviousEntitiesShouldDelegateToEntityManagerWithCorrectParameters()
        {
            _client.GetPreviousEntities<object>("collection", "query");

            _entityManager.Received(1).GetPreviousEntities<object>("collection", "query");
        }

        [Test]
        public void UpdateEntityShouldDelegateToEntityManagerWithCorrectParameters()
        {
            var entity = new {Name = "test"};

            _client.UpdateEntity<object>("collection", "identifier", entity);

            _entityManager.Received(1).UpdateEntity<object>("collection", "identifier", entity);
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void UpdateEntityShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.UpdateEntity(Arg.Any<string>(), Arg.Any<string>(), Arg.Any<object>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.UpdateEntity<object>(null, null, null);
        }
    }
}
