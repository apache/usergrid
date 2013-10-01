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