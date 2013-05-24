using Newtonsoft.Json;
using NSubstitute;
using RestSharp;
using System.Net;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests
{
    public static class Helpers
    {
        public static string Serialize(this object obj)
        {
            return JsonConvert.SerializeObject(obj);
        }

        public static IRestResponse<T> SetUpRestResponseWithContent<T>(HttpStatusCode httpStatusCode, object responseContent)
        {
            var restResponse = Substitute.For<IRestResponse<T>>();
            restResponse.StatusCode.Returns(HttpStatusCode.OK);
            restResponse.Content.Returns(responseContent.Serialize());
            return restResponse;
        }

        public static IRestResponse<T> SetUpRestResponseWithData<T>(HttpStatusCode httpStatusCode, T responseData)
        {
            var restResponse = Substitute.For<IRestResponse<T>>();
            restResponse.StatusCode.Returns(HttpStatusCode.OK);
            restResponse.Data.Returns(responseData);
            return restResponse;
        }

		public static IUsergridRequest InitializeUserGridRequestWithAccessToken()
		{
			var loginResponse = SetUpRestResponseWithData(HttpStatusCode.OK, new LoginResponse {AccessToken = "access_token"});

            var request = Substitute.For<IUsergridRequest>();
			request
				.Execute<LoginResponse> (Arg.Any<string>(), Arg.Any<Method> (), Arg.Any<object> (), Arg.Any<string> ())
					.Returns (loginResponse);

			return request;
		}
    }


}
