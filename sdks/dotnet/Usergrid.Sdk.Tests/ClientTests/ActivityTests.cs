using NSubstitute;
using NUnit.Framework;
using Usergrid.Sdk.Manager;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests.ClientTests
{
    [TestFixture]
    public class ActivityTests
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
        public void GetGroupActivitiesShouldDelegateToEntityManagerWithCorrectEndpoint()
        {
            _client.GetGroupActivities<UsergridActivity>("groupIdentifier");

            _entityManager.Received(1).GetEntities<UsergridActivity>("/groups/groupIdentifier/activities");
        }

        [Test]
        public void GetUserActivitiesShouldDelegateToEntityManagerWithCorrectEndpoint()
        {
            _client.GetUserActivities<UsergridActivity>("userIdentifier");

            _entityManager.Received(1).GetEntities<UsergridActivity>("/users/userIdentifier/activities");
        }

        [Test]
        public void PostActivityShouldDelegateToEntityManagerWithCorrectEndpoint()
        {
            var usergridActivity = new UsergridActivity();

            _client.PostActivity("userIdentifier", usergridActivity);

            _entityManager.Received(1).CreateEntity("/users/userIdentifier/activities", usergridActivity);
        }

        [Test]
        public void PostActivityToGroupShouldDelegateToEntityManagerWithCorrectEndpoint()
        {
            var usergridActivity = new UsergridActivity();

            _client.PostActivityToGroup("groupIdentifier", usergridActivity);

            _entityManager.Received(1).CreateEntity("/groups/groupIdentifier/activities", usergridActivity);
        }

        [Test]
        public void PostActivityToUsersFollowersInGroupShouldDelegateToEntityManagerWithCorrectEndpoint()
        {
            var usergridActivity = new UsergridActivity();

            _client.PostActivityToUsersFollowersInGroup("userIdentifier", "groupIdentifier", usergridActivity);

            _entityManager.Received(1).CreateEntity("/groups/groupIdentifier/users/userIdentifier/activities", usergridActivity);
        }
    }
}