using System.Collections.Generic;
using System.Net;
using NSubstitute;
using NUnit.Framework;
using RestSharp;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests
{
    [TestFixture]
    public class GetEntityTests
    {
        [Test]
        public void ShouldGetToCorrectEndPoint()
        {
            var restResponseContent = new UsergridGetResponse<Friend> { Entities = new List<Friend>(), Cursor = "" };
            IRestResponse<UsergridGetResponse<Friend>> restResponse = Helpers.SetUpRestResponseWithContent<UsergridGetResponse<Friend>>(HttpStatusCode.OK, restResponseContent);

            var request = Substitute.For<IUsergridRequest>();
            request
                .Execute(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>(), Arg.Any<string>())
                .Returns(restResponse);

            const string collectionName = "collection";
            const string entityName = "entity";

            var client = new Client(null, null, request: request);
            client.GetEntity<Friend>(collectionName, entityName);

            request.Received(1).Execute(
                Arg.Is(string.Format("/{0}/{1}", collectionName, entityName)),
                Arg.Is(Method.GET),
                Arg.Any<object>(),
                Arg.Any<string>());
        }

        [Test]
        public void ShouldPassCorrectAccessToken()
        {
            const string accessToken = "access_token";
            IUsergridRequest request = Helpers.InitializeUserGridRequestWithAccessToken(accessToken);

            var restResponseContent = new UsergridGetResponse<Friend> { Entities = new List<Friend>(), Cursor = "" };
            IRestResponse<UsergridGetResponse<Friend>> restResponse = Helpers.SetUpRestResponseWithContent<UsergridGetResponse<Friend>>(HttpStatusCode.OK, restResponseContent);

            request
                .Execute(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>(), Arg.Any<string>())
                .Returns(restResponse);

            const string collectionName = "collection";
            const string entityName = "entity";

            var client = new Client(null, null, request: request);
            client.Login(null, null, AuthType.ClientId);
            var friend = client.GetEntity<Friend>(collectionName, entityName);
            Assert.IsNull(friend);

            request.Received(1).Execute(
                Arg.Is(string.Format("/{0}/{1}", collectionName, entityName)),
                Arg.Is(Method.GET),
                Arg.Any<object>(),
                accessToken);
        }

        [Test]
        public void ShouldReturnEntityCorrectly()
        {
            var friend = new Friend { Name = "name", Age = 1 };

            var restResponseContent = new UsergridGetResponse<Friend> { Entities = new List<Friend> { friend }, Cursor = "cursor" };
            IRestResponse<UsergridGetResponse<Friend>> restResponse = Helpers.SetUpRestResponseWithContent<UsergridGetResponse<Friend>>(HttpStatusCode.OK, restResponseContent);

            var request = Substitute.For<IUsergridRequest>();
            request
                .Execute(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>(), Arg.Any<string>())
                .Returns(restResponse);

            const string collectionName = "collection";
            const string entityName = "entity";

            var client = new Client(null, null, request: request);
            var returnedFriend = client.GetEntity<Friend>(collectionName, entityName);

            Assert.IsNotNull(returnedFriend);
            Assert.AreEqual(friend.Name, returnedFriend.Name);
            Assert.AreEqual(friend.Age, returnedFriend.Age);
        }

        [Test]
        public void ShouldReturnFirstEntityInListCorrectly()
        {
            var friend1 = new Friend { Name = "name1", Age = 1 };
            var friend2 = new Friend { Name = "name2", Age = 2 };

            var entities = new List<Friend> { friend1, friend2 };
            var restResponseContent = new UsergridGetResponse<Friend> { Entities = entities, Cursor = "cursor" };

            IRestResponse<UsergridGetResponse<Friend>> restResponse = Helpers.SetUpRestResponseWithContent<UsergridGetResponse<Friend>>(HttpStatusCode.OK, restResponseContent);

            var request = Substitute.For<IUsergridRequest>();
            request
                .Execute(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>(), Arg.Any<string>())
                .Returns(restResponse);

            const string collectionName = "collection";
            const string entityName = "entity";

            var client = new Client(null, null, request: request);
            var returnedFriend = client.GetEntity<Friend>(collectionName, entityName);

            Assert.IsNotNull(returnedFriend);
            Assert.AreEqual(friend1.Name, returnedFriend.Name);
            Assert.AreEqual(friend1.Age, returnedFriend.Age);
        }

        [Test]
        public void ShouldReturnNullEntityCorrectly()
        {
            var entities = new List<Friend> { null };

            var restResponseContent = new UsergridGetResponse<Friend> { Entities = entities, Cursor = "cursor" };
            IRestResponse<UsergridGetResponse<Friend>> restResponse = Helpers.SetUpRestResponseWithContent<UsergridGetResponse<Friend>>(HttpStatusCode.OK, restResponseContent);

            var request = Substitute.For<IUsergridRequest>();
            request
                .Execute(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>(), Arg.Any<string>())
                .Returns(restResponse);

            const string collectionName = "collection";
            const string entityName = "entity";

            var client = new Client(null, null, request: request);
            var returnedFriend = client.GetEntity<Friend>(collectionName, entityName);

            Assert.IsNull(returnedFriend);
        }
    }

    public class Friend
    {
        public string Name { get; set; }
        public int Age { get; set; }
    }
}