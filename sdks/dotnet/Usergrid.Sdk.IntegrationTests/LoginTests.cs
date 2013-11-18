using NUnit.Framework;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.IntegrationTests
{
    [TestFixture]
    public class LoginTests : BaseTest
    {
        [Test]
        public void ShouldLoginSuccessfullyWithClientCredentials()
        {
            var client = new Client(Organization, Application);
            client.Login(ClientId, ClientSecret, AuthType.Organization);
        }

		[Test]
		public void ShouldThrowWithInvalidOrganizationCredentials()
		{
			var client = new Client (Organization, Application);

			try
			{
				client.Login("Invalid_User_Name", "Invalid_Password", AuthType.Organization);
				Assert.True(true, "Was expecting login to throw UserGridException");
			}
			catch (UsergridException e)
			{
				Assert.That (e.Message, Is.EqualTo ("invalid username or password"));
				Assert.That(e.ErrorCode, Is.EqualTo("invalid_grant"));
			}
		}

		[Test]
		public void ShouldLoginSuccessfullyWithApplicationCredentials()
		{
			var client = new Client(Organization, Application);
			client.Login(ApplicationId, ApplicationSecret, AuthType.Application);
		}

		[Test]
		public void ShouldThrowWithInvalidApplicationCredentials()
		{
			var client = new Client (Organization, Application);

			try
			{
				client.Login("Invalid_User_Name", "Invalid_Password", AuthType.Application);
				Assert.True(true, "Was expecting login to throw UserGridException");
			}
			catch (UsergridException e)
			{
				Assert.That (e.Message, Is.EqualTo ("invalid username or password"));
				Assert.That(e.ErrorCode, Is.EqualTo("invalid_grant"));
			}
		}

		[Test]
		public void ShouldLoginSuccessfullyWithUserCredentials()
		{
			var client = new Client(Organization, Application);
			client.Login(UserId, UserSecret, AuthType.User);
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
				Assert.That (e.Message, Is.EqualTo ("invalid username or password"));
                Assert.That(e.ErrorCode, Is.EqualTo("invalid_grant"));
            }
        }
    }
}