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


using System.Net;
using System.Reflection;
using NSubstitute;
using Newtonsoft.Json;
using RestSharp;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Tests
{
	internal static class Helpers
    {
		internal static string Serialize(this object obj)
        {
            return JsonConvert.SerializeObject(obj);
        }

		internal static IRestResponse<T> SetUpRestResponseWithContent<T>(HttpStatusCode httpStatusCode, object responseContent)
		{
		    return SetUpRestResponseWithContent<T>(httpStatusCode, responseContent.Serialize());
		}
		
        internal static IRestResponse<T> SetUpRestResponseWithContent<T>(HttpStatusCode httpStatusCode, string responseContent)
        {
            var restResponse = Substitute.For<IRestResponse<T>>();
            restResponse.StatusCode.Returns(httpStatusCode);
            restResponse.Content.Returns(responseContent);
            return restResponse;
        }

        internal static IRestResponse SetUpRestResponse(HttpStatusCode httpStatusCode)
        {
            var restResponse = Substitute.For<IRestResponse>();
            restResponse.StatusCode.Returns(httpStatusCode);
            return restResponse;
        }

		internal static IRestResponse<T> SetUpRestResponseWithData<T>(HttpStatusCode httpStatusCode, T responseData)
        {
            var restResponse = Substitute.For<IRestResponse<T>>();
            restResponse.StatusCode.Returns(httpStatusCode);
            restResponse.Data.Returns(responseData);
            return restResponse;
        }

        internal static IUsergridRequest SetUpUsergridRequestWithRestResponse<T>(IRestResponse<T> restResponse) where T : new()
        {
            var request = Substitute.For<IUsergridRequest>();
            request
                .ExecuteJsonRequest<T>(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(restResponse);

            return request;
        }

		internal static IUsergridRequest InitializeUserGridRequestWithAccessToken(string accessToken)
        {
            IRestResponse<LoginResponse> loginResponse = SetUpRestResponseWithData(HttpStatusCode.OK, new LoginResponse {AccessToken = accessToken});

            var request = Substitute.For<IUsergridRequest>();
            request
                .ExecuteJsonRequest<LoginResponse>(Arg.Any<string>(), Arg.Any<Method>(), Arg.Any<object>())
                .Returns(loginResponse);

            return request;
        }

        public static object GetReflectedProperty(this object obj, string propertyName)
        {
            PropertyInfo property = obj.GetType().GetProperty(propertyName);

            if (property == null)
                return null;

            return property.GetValue(obj, null);
        }
    }
}
