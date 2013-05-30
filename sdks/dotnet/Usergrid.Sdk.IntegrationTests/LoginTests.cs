using NUnit.Framework;

namespace Usergrid.Sdk.IntegrationTests
{
    [TestFixture]
    public class LoginTests : BaseTest
    {
        [Test]
        public void ShouldLoginWithClientCredentialsAndSetTheAccessToken()
        {
            var client = new Client(Organization, Application);
            client.Login(ClientId, ClientSecret, AuthType.ClientId);

            Assert.IsFalse(string.IsNullOrEmpty(client.AccessToken));
        }

        [Test]
        public void ShouldLoginWithUserCredentialsAndSetTheAccessToken()
        {
            var client = new Client(Organization, Application);
            client.Login(UserId, UserSecret, AuthType.User);

            Assert.IsFalse(string.IsNullOrEmpty(client.AccessToken));
        }

        [Test]
        public void ShouldThrowWithInvalidUserCredentials()
        {
            var client = new Client(Organization, Application);

            try
            {
                client.Login("Invalid_User_Name", "Invalid_Password", AuthType.User);
                Assert.True(true, "Was expecting login to throw UserGridException");
            }
            catch (UsergridException e)
            {
                Assert.That(e.ErrorCode, Is.EqualTo("invalid_grant"));
            }
        }
    }
}