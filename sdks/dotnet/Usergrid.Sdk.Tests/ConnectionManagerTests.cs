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
    public class ConnectionManagerTests
    {
        [SetUp]
        public void Setup()
        {
            _request = Substitute.For<IUsergridRequest>();
            _connectionManager = new ConnectionManager(_request);
        }

        private IUsergridRequest _request;
        private ConnectionManager _connectionManager;

        [Test]
        public void CreateConnectionShouldPostToCorrectEndpoint() {
            var connection = new Connection
                {
                    ConnectorCollectionName = "users",
                    ConnectorIdentifier = "userName",
                    ConnecteeCollectionName = "devices",
                    ConnecteeIdentifier = "deviceName",
                    ConnectionName = "has"
                };
            IRestResponse restResponse = Helpers.SetUpRestResponse(HttpStatusCode.OK);

            _request
                .ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponse);

            _connectionManager.CreateConnection(connection);

            _request
                .Received(1)
                .ExecuteJsonRequest(
                    "/users/userName/has/devices/deviceName",
                    Method.POST);
        }

        [Test]
        public void CreateConnectionShouldThrowUsergridExceptionWhenBadResponse() {
            var connection = new Connection();
            var restResponseContent = new UsergridError {Description = "Exception message", Error = "error code"};
            IRestResponse<LoginResponse> restResponseWithBadRequest = Helpers.SetUpRestResponseWithContent<LoginResponse>(HttpStatusCode.BadRequest, restResponseContent);

            _request
                .ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponseWithBadRequest);

            try
            {
                _connectionManager.CreateConnection(connection);
                new AssertionException("UserGridException was expected to be thrown here");
            }
            catch (UsergridException e)
            {
                Assert.AreEqual("error code", e.ErrorCode);
                Assert.AreEqual("Exception message", e.Message);
            }
        }

        [Test]
        public void GetConnectionsReturnsConnectionsAsList()
        {
            var connection = new Connection
            {
                ConnectorCollectionName = "users",
                ConnectorIdentifier = "userName",
                ConnectionName = "has"
            };

            var expectedEntities = new List<UsergridEntity>();
            var responseData = new UsergridGetResponse<UsergridEntity>() {Entities = expectedEntities};
            IRestResponse restResponse = Helpers.SetUpRestResponseWithContent<UsergridGetResponse<UsergridEntity>>(HttpStatusCode.OK, responseData);

            _request
                .ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponse);

            var returnedEntities = _connectionManager.GetConnections(connection);

            _request
                .Received(1)
                .ExecuteJsonRequest("/users/userName/has",Method.GET);
            Assert.AreEqual(expectedEntities, returnedEntities);
        }

        [Test]
        public void GetConnectionsReturnsNullWhenConnectionIsNotFound()
        {
            var connection = new Connection
            {
                ConnectorCollectionName = "users",
                ConnectorIdentifier = "userName",
                ConnectionName = "has"
            };
            IRestResponse restResponse = Helpers.SetUpRestResponse(HttpStatusCode.NotFound);

            _request
                .ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponse);

            var returnedEntities = _connectionManager.GetConnections(connection);

            _request
                .Received(1)
                .ExecuteJsonRequest("/users/userName/has",Method.GET);

            Assert.IsNull(returnedEntities);
        }

        [Test]
        public void GetConnectionsOfSpecificTypeReturnsConnectionsAsListOfConnecteeType()
        {
            var connection = new Connection
            {
                ConnectorCollectionName = "users",
                ConnectorIdentifier = "userName",
                ConnecteeCollectionName = "devices",
                ConnectionName = "has"
            };
            var expectedEntities = new List<UsergridDevice>();
            var responseData = new UsergridGetResponse<UsergridDevice>() { Entities = expectedEntities };
            IRestResponse restResponse = Helpers.SetUpRestResponseWithContent<UsergridGetResponse<UsergridDevice>>(HttpStatusCode.OK, responseData);

            _request
                .ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponse);

            var returnedEntities = _connectionManager.GetConnections<UsergridDevice>(connection);

            _request
                .Received(1)
                .ExecuteJsonRequest("/users/userName/has/devices", Method.GET);
            Assert.AreEqual(expectedEntities, returnedEntities);
        }

        [Test]
        public void GetConnectionsOfSpecificTypeReturnsNullWhenConnectionIsNotFound()
        {
            var connection = new Connection
            {
                ConnectorCollectionName = "users",
                ConnectorIdentifier = "userName",
                ConnecteeCollectionName = "devices",
                ConnectionName = "has"
            };
            IRestResponse restResponse = Helpers.SetUpRestResponse(HttpStatusCode.NotFound);

            _request
                .ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponse);

            var returnedEntities = _connectionManager.GetConnections<UsergridDevice>(connection);

            _request
                .Received(1)
                .ExecuteJsonRequest("/users/userName/has/devices",Method.GET);

            Assert.IsNull(returnedEntities);
        }


        [Test]
        public void DeleteConnectionShouldDeleteToCorrectEndpoint()
        {
            var connection = new Connection
            {
                ConnectorCollectionName = "users",
                ConnectorIdentifier = "userName",
                ConnecteeCollectionName = "devices",
                ConnecteeIdentifier = "deviceName",
                ConnectionName = "has"
            };
            IRestResponse restResponse = Helpers.SetUpRestResponse(HttpStatusCode.OK);

            _request
                .ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponse);

            _connectionManager.DeleteConnection(connection);

            _request
                .Received(1)
                .ExecuteJsonRequest(
                    "/users/userName/has/devices/deviceName",
                    Method.DELETE);
        }

        [Test]
        public void DeleteConnectionShouldThrowUsergridExceptionWhenBadResponse()
        {
            var connection = new Connection();
            var restResponseContent = new UsergridError { Description = "Exception message", Error = "error code" };
            IRestResponse<LoginResponse> restResponseWithBadRequest = Helpers.SetUpRestResponseWithContent<LoginResponse>(HttpStatusCode.BadRequest, restResponseContent);

            _request
                .ExecuteJsonRequest(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponseWithBadRequest);

            try
            {
                _connectionManager.DeleteConnection(connection);
                new AssertionException("UserGridException was expected to be thrown here");
            }
            catch (UsergridException e)
            {
                Assert.AreEqual("error code", e.ErrorCode);
                Assert.AreEqual("Exception message", e.Message);
            }
        }

    }
}
