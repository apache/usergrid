using NUnit.Framework;

namespace Usergrid.Sdk.IntegrationTests
{
    [TestFixture]
    public class LoginTests
    {
        //Organization API Credentials
        private const string ClientId = "__YOUR_CLIENT_ID_HERE__";
        private const string ClientSecret = "__YOUR_CLIENT_SECRET_HERE__";
        //User Login Credentials
        private const string UserLoginId = "__USER_NAME_HERE__";
        private const string UserSecret = "__USER_PASSWORD_HERE__";
        //Organization and Application Details
        private const string Organization = "__ORGANIZATION_NAME_HERE__";
        private const string Application = "__APPLICATION_NAME_HERE__";

        [Test]
        public void ShouldLoginWithClientCredentialsAndSetTheAccessToken()
        {
            var client = new Client(Organization, Application, AuthType.ClientId);
            client.Login(ClientId, ClientSecret);

            Assert.IsFalse(string.IsNullOrEmpty(client.AccessToken));
        }

        [Test]
        public void ShouldLoginWithUserCredentialsAndSetTheAccessToken()
        {
            var client = new Client(Organization, Application, AuthType.User);
            client.Login(UserLoginId, UserSecret);

            Assert.IsFalse(string.IsNullOrEmpty(client.AccessToken));
        }

        [Test]
        public void ShouldThrowWithInvalidUserCredentials()
        {
            var client = new Client(Organization, Application, AuthType.User);

            try
            {
                client.Login("Invalid_User_Name", "Invalid_Password");
                Assert.True(true, "Was expecting login to throw UserGridException");
            }
            catch (UsergridException e)
            {
                Assert.That(e.ErrorCode, Is.EqualTo("invalid_grant"));
            }
        }
    }
}