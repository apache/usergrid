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

using RestSharp;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Manager
{
    internal class AuthenticationManager : ManagerBase, IAuthenticationManager
    {
        public AuthenticationManager(IUsergridRequest request) : base(request)
        {
        }

        public void ChangePassword(string userName, string oldPassword, string newPassword)
        {
            var payload = new ChangePasswordPayload {OldPassword = oldPassword, NewPassword = newPassword};
            IRestResponse response = Request.ExecuteJsonRequest(string.Format("/users/{0}/password", userName), Method.POST, payload);
            ValidateResponse(response);
        }

        public void Login(string loginId, string secret, AuthType authType)
        {
            if (authType == AuthType.None)
            {
                Request.AccessToken = null;
                return;
            }

            object body = GetLoginBody(loginId, secret, authType);

            IRestResponse<LoginResponse> response = Request.ExecuteJsonRequest<LoginResponse>("/token", Method.POST, body);
            ValidateResponse(response);

            Request.AccessToken = response.Data.AccessToken;
        }

        private object GetLoginBody(string loginId, string secret, AuthType authType)
        {
            object body = null;

            if (authType == AuthType.Organization || authType == AuthType.Application)
            {
                body = new ClientIdLoginPayload
                    {
                        ClientId = loginId,
                        ClientSecret = secret
                    };
            }
            else if (authType == AuthType.User)
            {
                body = new UserLoginPayload
                    {
                        UserName = loginId,
                        Password = secret
                    };
            }
            return body;
        }
    }
}
