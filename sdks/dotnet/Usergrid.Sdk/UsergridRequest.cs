using RestSharp;

namespace Usergrid.Sdk
{
    public class UsergridRequest : IUsergridRequest
    {
        private readonly string _organization;
        private readonly string _application;
        private readonly RestClient _restClient;

        public UsergridRequest(string baseUri, string organization, string application)
        {
            _organization = organization;
            _application = application;
            _restClient = new RestClient(baseUri);
        }

        public IRestResponse<T> Execute<T>(string resource, Method method, object body = null, string accessToken=null) where T : new()
        {
            var request = GetRequest(resource, method, body, accessToken);
            IRestResponse<T> response = _restClient.Execute<T>(request);
            return    response;
        }

        public IRestResponse Execute(string resource, Method method, object body = null, string accessToken = null) 
        {
            var request = GetRequest(resource, method, body, accessToken);
            IRestResponse response = _restClient.Execute(request);
            return    response;
        }

        private RestRequest GetRequest(string resource, Method method, object body, string accessToken)
        {
            var request = new RestRequest(string.Format("{0}/{1}{2}", _organization, _application, resource), method);
            request.JsonSerializer = new RestSharpJsonDeserializer();
            AddAuthorizationHeader(accessToken, request);
			if (body != null)
			{
				request.RequestFormat = DataFormat.Json;
            	request.AddBody(body);
			}
            return request;
        }

        private void AddAuthorizationHeader(string accessToken, RestRequest request)
        {
            if (accessToken != null)
                request.AddHeader("Authorization", string.Format("Bearer {0}", accessToken));
        }
    }
}