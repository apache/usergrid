using RestSharp;

namespace Usergrid.Sdk
{
    public interface IUsergridRequest
    {
        IRestResponse<T> Execute<T>(string resource, Method method, object body = null, string accessToken=null) where T : new();
        IRestResponse Execute(string resource, Method method, object body = null, string accessToken = null);
    }
}