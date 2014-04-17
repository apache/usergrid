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
using NSubstitute;
using NUnit.Framework;
using Usergrid.Sdk.Manager;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests.ClientTests
{
    [TestFixture]
    public class ConnectionTests
    {
        #region Setup/Teardown

        [SetUp]
        public void Setup()
        {
            _connectionManager = Substitute.For<IConnectionManager>();
            _client = new Client(null, null) {ConnectionManager = _connectionManager};
        }

        #endregion

        private IConnectionManager _connectionManager;
        private IClient _client;

        [Test]
        public void CreateConnectionShouldDelegateToConnectionManagerWithCorrectParameters()
        {
            var connection = new Connection();
            _client.CreateConnection(connection);

            _connectionManager.Received(1).CreateConnection(connection);
        }

        [Test]
        public void DeleteConnectionsShouldDelegateToConnectionManagerWithCorrectParameters()
        {
            var connection = new Connection();

            _client.DeleteConnection(connection);

            _connectionManager.Received(1).DeleteConnection(connection);
        }

        [Test]
        public void GetConnectionsOfSpecificTypeShouldDelegateToConnectionManagerWithCorrectParameters()
        {
            var connection = new Connection();
            var enityList = new List<UsergridEntity>();

            _connectionManager.GetConnections<UsergridEntity>(connection).Returns(enityList);

            IList<UsergridEntity> returnedEntityList = _client.GetConnections<UsergridEntity>(connection);

            _connectionManager.Received(1).GetConnections<UsergridEntity>(connection);
            Assert.AreEqual(enityList, returnedEntityList);
        }

        [Test]
        public void GetConnectionsShouldDelegateToConnectionManagerWithCorrectParameters()
        {
            var connection = new Connection();
            var enityList = new List<UsergridEntity>();
            _connectionManager.GetConnections(connection).Returns(enityList);

            IList<UsergridEntity> returnedEntityList = _client.GetConnections(connection);

            _connectionManager.Received(1).GetConnections(connection);
            Assert.AreEqual(enityList, returnedEntityList);
        }
    }
}
