using System.Collections.Generic;
using RestSharp;

namespace Usergrid.Sdk
{
    public interface IUsergridRequest
    {
        IRestResponse<T> ExecuteJsonRequest<T>(string resource, Method method, object body = null) where T : new();
        IRestResponse ExecuteJsonRequest(string resource, Method method, object body = null);
        IRestResponse ExecuteMultipartFormDataRequest(string resource, Method method, IDictionary<string, object> formParameters, IDictionary<string, string> fileParameters);
        string AccessToken { get; set; }
    }
}