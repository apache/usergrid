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
            var client = new Client(Organization, Application, ApiUri);
            client.Login(ClientId, ClientSecret, AuthType.Organization);
        }

		[Test]
		public void ShouldThrowWithInvalidOrganizationCredentials()
		{
			var client = new Client (Organization, Application, ApiUri);

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
			var client = new Client(Organization, Application, ApiUri);
			client.Login(ApplicationId, ApplicationSecret, AuthType.Application);
		}

		[Test]
		public void ShouldThrowWithInvalidApplicationCredentials()
		{
			var client = new Client (Organization, Application, ApiUri);

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
			var client = new Client(Organization, Application, ApiUri);
			client.Login(UserId, UserSecret, AuthType.User);
		}

        [Test]
        public void ShouldThrowWithInvalidUserCredentials()
        {
            var client = new Client(Organization, Application, ApiUri);

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
