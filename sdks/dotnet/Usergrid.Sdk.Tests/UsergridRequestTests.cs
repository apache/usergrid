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


using System;
using System.Collections.Generic;
using NSubstitute;
using NUnit.Framework;
using RestSharp;
using RestSharp.Serializers;

namespace Usergrid.Sdk.Tests
{
    [TestFixture]
    public class UsergridRequestTests
    {
        [Test]
        public void ExecuteJsonRequestAddsAuthorizationHeader()
        {
            var restClient = Substitute.For<IRestClient>();
            var usergridRequest = new UsergridRequest("http://usergrid.com", "org", "app", restClient);
            usergridRequest.AccessToken = "accessToken";

            usergridRequest.ExecuteJsonRequest("/resource", Method.POST);


            Func<RestRequest, bool> ValidateRequest = r => r.Parameters[0].Name == "Authorization" &&
                                                           (string)r.Parameters[0].Value == "Bearer accessToken" &&
                                                           r.Parameters[0].Type == ParameterType.HttpHeader &&
                                                           r.Method == Method.POST;
            restClient.Received(1).Execute(Arg.Is<RestRequest>(r=>ValidateRequest(r)));
        }

        [Test]
        public void GenericExecuteJsonRequestAddsAuthorizationHeader()
        {
            var restClient = Substitute.For<IRestClient>();
            var usergridRequest = new UsergridRequest("http://usergrid.com", "org", "app", restClient);
            usergridRequest.AccessToken = "accessToken";

            usergridRequest.ExecuteJsonRequest<object>("/resource", Method.POST);


            Func<RestRequest, bool> ValidateRequest = r => r.Parameters[0].Name == "Authorization" &&
                                                           (string)r.Parameters[0].Value == "Bearer accessToken" &&
                                                           r.Parameters[0].Type == ParameterType.HttpHeader &&
                                                           r.Method == Method.POST;
            restClient.Received(1).Execute<object>(Arg.Is<RestRequest>(r=>ValidateRequest(r)));
        }

        [Test]
        public void ExecuteJsonRequestAddsBodyAsJsonAndSetsSerializer()
        {
            var restClient = Substitute.For<IRestClient>();
            var body = new {data="test"};
            var usergridRequest = new UsergridRequest("http://usergrid.com", "org", "app", restClient);

            usergridRequest.ExecuteJsonRequest("/resource", Method.POST, body);


            Func<RestRequest, bool> ValidateRequest = r => 
                (string)r.Parameters[0].Value == "{\"data\":\"test\"}"&& 
                r.RequestFormat == DataFormat.Json &&
                r.JsonSerializer is RestSharpJsonSerializer;
            restClient.Received(1).Execute(Arg.Is<RestRequest>(r => ValidateRequest(r)));
        }

        [Test]
        public void GenericExecuteJsonRequestAddsBodyAsJsonAndSetsSerializer()
        {
            var restClient = Substitute.For<IRestClient>();
            var body = new {data="test"};
            var usergridRequest = new UsergridRequest("http://usergrid.com", "org", "app", restClient);

            usergridRequest.ExecuteJsonRequest<object>("/resource", Method.POST, body);


            Func<RestRequest, bool> ValidateRequest = r => 
                (string)r.Parameters[0].Value == "{\"data\":\"test\"}"&& 
                r.RequestFormat == DataFormat.Json &&
                r.JsonSerializer is RestSharpJsonSerializer;
            restClient.Received(1).Execute<object>(Arg.Is<RestRequest>(r => ValidateRequest(r)));
        }

        [Test]
        public void ExecuteMultipartFormDataRequestAddsFormsParametersAndFileParameters()
        {
            var restClient = Substitute.For<IRestClient>();
            var usergridRequest = new UsergridRequest("http://usergrid.com", "org", "app", restClient);

            IDictionary<string, object> formParameters = new Dictionary<string, object>()
                                                             {
                                                                 {"param1", "value1"},
                                                                 {"param2", "value2"}
                                                             };

            IDictionary<string, string> fileParameters = new Dictionary<string, string>()
                                                             {
                                                                 {"fileParam1", "filePath1"}
                                                             };

            usergridRequest.ExecuteMultipartFormDataRequest("/resource", Method.POST, formParameters, fileParameters);


            Func<RestRequest, bool> ValidateRequest = r =>
                                                          {
                                                              bool isValid = true;
                                                              isValid &= r.Parameters[0].Name == "param1";
                                                              isValid &= (string)r.Parameters[0].Value == "value1";
                                                              isValid &= r.Parameters[0].Type == ParameterType.GetOrPost;

                                                              isValid &= r.Parameters[1].Name == "param2";
                                                              isValid &= (string)r.Parameters[1].Value == "value2";
                                                              isValid &= r.Parameters[1].Type == ParameterType.GetOrPost;

                                                              isValid &= r.Files[0].Name == "fileParam1";
                                                              isValid &= r.Files[0].FileName == "filePath1";

                                                              return isValid;
                                                          };


            restClient.Received(1).Execute(Arg.Is<RestRequest>(r => ValidateRequest(r)));


        }
    }
}
