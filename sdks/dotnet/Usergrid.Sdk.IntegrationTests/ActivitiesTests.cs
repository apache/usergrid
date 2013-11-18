using System;
using NUnit.Framework;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.IntegrationTests
{
	[TestFixture]
	public class ActivitiesTests : BaseTest
	{
        private IClient _client;
        [SetUp]
        public void Setup()
        {
            _client = InitializeClientAndLogin(AuthType.Organization);
        }

		[Test]
		public void ShouldCreateAndRetrieveUserActivities()
		{
            // Create a user
            var usergridUser = SetupUsergridUser(_client, new UsergridUser { UserName = "test_user", Email = "test_user@apigee.com" });
			// Create an activity for this user
			var activityEntity = new UsergridActivity {
				Actor = new UsergridActor
				{
					DisplayName = "Joe Doe",
                    Email = usergridUser.Email,
                    UserName = usergridUser.UserName,
                    Uuid = usergridUser.Uuid,
					Image = new UsergridImage
					{
						Height = 10,
						Width = 20,
						Duration = 0,
						Url = "apigee.com"
					}
				},
				Content = "Hello Usergrid",
				Verb = "post"
			};

            _client.PostActivity(usergridUser.UserName, activityEntity);

			// Get the activities
            var activities = _client.GetUserActivities<UsergridActivity>(usergridUser.UserName);
			Assert.IsNotNull (activities);
			Assert.AreEqual (1, activities.Count);
			var thisActivity = activities [0];
			Assert.AreEqual ("Joe Doe", thisActivity.Actor.DisplayName);
            Assert.AreEqual(usergridUser.Email, thisActivity.Actor.Email);
			Assert.AreEqual (10, thisActivity.Actor.Image.Height);
			Assert.AreEqual (20, thisActivity.Actor.Image.Width);
			Assert.AreEqual ("Hello Usergrid", thisActivity.Content);
			Assert.IsTrue (thisActivity.PublishedDate > DateTime.Now.ToUniversalTime().AddHours(-1));

			// Get the feed
            var feed = _client.GetUserFeed<UsergridActivity>(usergridUser.UserName);
			Assert.IsNotNull (feed);
			Assert.AreEqual (1, feed.Count);
			thisActivity = feed [0];
			Assert.AreEqual ("Joe Doe", thisActivity.Actor.DisplayName);
            Assert.AreEqual(usergridUser.Email, thisActivity.Actor.Email);
			Assert.AreEqual (10, thisActivity.Actor.Image.Height);
			Assert.AreEqual (20, thisActivity.Actor.Image.Width);
			Assert.AreEqual ("Hello Usergrid", thisActivity.Content);
			Assert.IsTrue (thisActivity.PublishedDate > DateTime.Now.ToUniversalTime().AddHours(-1));
		}

		[Test]
		public void ShouldCreateAndRetrieveGroupActivities()
		{

            // Create a user
            var usergridUser = SetupUsergridUser(_client, new UsergridUser { UserName = "test_user", Email = "test_user@apigee.com" });
            // Create a group
            var usergridGroup = SetupUsergridGroup(_client, new UsergridGroup { Path = "test-group", Title = "mygrouptitle" });

			// Create an activity for this group
			var activityEntity = new UsergridActivity {
				Actor = new UsergridActor
				{
					DisplayName = "Joe Doe",
                    Email = usergridUser.Email,
                    UserName = usergridUser.UserName,
                    Uuid = usergridUser.Uuid,
					Image = new UsergridImage
					{
						Height = 10,
						Width = 20,
						Duration = 0,
						Url = "apigee.com"
					}
				},
				Content = "Hello Usergrid",
				Verb = "post"
			};

            _client.PostActivityToGroup(usergridGroup.Path, activityEntity);

			// Get the activities
            var activities = _client.GetGroupActivities<UsergridActivity>(usergridGroup.Path);
			Assert.IsNotNull (activities);
			Assert.AreEqual (1, activities.Count);
			var thisActivity = activities [0];
			Assert.AreEqual ("Joe Doe", thisActivity.Actor.DisplayName);
			Assert.AreEqual (usergridUser.Email, thisActivity.Actor.Email);
			Assert.AreEqual (10, thisActivity.Actor.Image.Height);
			Assert.AreEqual (20, thisActivity.Actor.Image.Width);
			Assert.AreEqual ("Hello Usergrid", thisActivity.Content);
			Assert.IsTrue (thisActivity.PublishedDate > DateTime.Now.ToUniversalTime().AddHours(-1));

			// Get the feed
            var feed = _client.GetGroupFeed<UsergridActivity>(usergridGroup.Path);
			Assert.IsNotNull (feed);
			Assert.AreEqual (1, feed.Count);
			thisActivity = feed [0];
			Assert.AreEqual ("Joe Doe", thisActivity.Actor.DisplayName);
            Assert.AreEqual(usergridUser.Email, thisActivity.Actor.Email);
			Assert.AreEqual (10, thisActivity.Actor.Image.Height);
			Assert.AreEqual (20, thisActivity.Actor.Image.Width);
			Assert.AreEqual ("Hello Usergrid", thisActivity.Content);
			Assert.IsTrue (thisActivity.PublishedDate > DateTime.Now.ToUniversalTime().AddHours(-1));
		}
	
		[Test]
		public void ShouldCreateAndRetrieveUsersFollowersActivities()
		{
			

            // Create a user
            var usergridUser = SetupUsergridUser(_client, new UsergridUser { UserName = "test_user", Email = "test_user@apigee.com" });
            // Create a group
            var usergridGroup = SetupUsergridGroup(_client, new UsergridGroup { Path = "test-group", Title = "mygrouptitle" });


			// Create an activity for this group
            var activityEntity = new UsergridActivity(usergridUser)
            {
				Content = "Hello Usergrid",
				Verb = "post"
			};

            _client.PostActivityToUsersFollowersInGroup(usergridUser.UserName, usergridGroup.Path, activityEntity);

			// Get the activities
            var activities = _client.GetUserActivities<UsergridActivity>(usergridUser.UserName);
			Assert.IsNotNull (activities);
			Assert.AreEqual (1, activities.Count);
			var thisActivity = activities [0];
            Assert.AreEqual(usergridUser.Name, thisActivity.Actor.DisplayName);
            Assert.AreEqual(usergridUser.Email, thisActivity.Actor.Email);
			Assert.IsNull (thisActivity.Actor.Image);
			Assert.AreEqual ("Hello Usergrid", thisActivity.Content);
			Assert.IsTrue (thisActivity.PublishedDate > DateTime.Now.ToUniversalTime().AddHours(-1));
		}
	}

}

