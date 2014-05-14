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

using NSubstitute;
using NUnit.Framework;
using Usergrid.Sdk.Manager;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests.ClientTests
{
    [TestFixture]
    public class LoginTests
    {
        [TestCase("login1", "secret1", AuthType.Application)]
        [TestCase("login2", "secret2", AuthType.Organization)]
        [TestCase("login3", "secret3", AuthType.None)]
        [TestCase("login4", "secret4", AuthType.User)]
        public void LoginShouldDelegateToAuthenticationManagerWithCorrectParameters(string login, string secret, AuthType authType)
        {
            var authenticationManager = Substitute.For<IAuthenticationManager>();

            var client = new Client(null, null) {AuthenticationManager = authenticationManager};

            client.Login(login, secret, authType);

            authenticationManager.Received(1).Login(login, secret, authType);
        }

        [Test]
        public void LoginShouldDelegateToAuthenticationManager()
        {
            var authenticationManager = Substitute.For<IAuthenticationManager>();

            var client = new Client(null, null) {AuthenticationManager = authenticationManager};

            client.Login(null, null, AuthType.None);

            authenticationManager.Received(1).Login(null, null, AuthType.None);
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void LoginShouldPassOnTheException()
        {
            var authenticationManager = Substitute.For<IAuthenticationManager>();
            authenticationManager
                .When(m => m.Login(null, null, AuthType.None))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });


            var client = new Client(null, null) {AuthenticationManager = authenticationManager};
            client.Login(null, null, AuthType.None);
        }
    }
}
