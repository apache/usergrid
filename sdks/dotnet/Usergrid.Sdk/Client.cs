using System.Linq;
using System.Net;
using Newtonsoft.Json;
using RestSharp;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk
{
    public class Client
    {
        private const string UserGridEndPoint = "https://api.usergrid.com";
        private readonly IUsergridRequest _request;

        public Client(string organization, string application)
            : this(organization, application, UserGridEndPoint, new UsergridRequest(UserGridEndPoint, organization, application))
        {
        }

        public Client(string organization, string application, string uri = UserGridEndPoint)
            : this(organization, application, uri, new UsergridRequest(uri, organization, application))
        {
        }

        internal Client(string organization, string application, string uri = UserGridEndPoint, IUsergridRequest request = null)
        {
            _request = request ?? new UsergridRequest(uri, organization, application);
        }

        public string AccessToken { get; private set; }

        public void Login(string loginId, string secret, AuthType authType)
        {
            if (authType == AuthType.None)
            {
                AccessToken = null;
                return;
            }

            var body = GetLoginBody(loginId, secret, authType);

            var response = _request.Execute<LoginResponse>("/token", Method.POST, body);
            ValidateResponse(response);

            AccessToken = response.Data.AccessToken;
        }

        private object GetLoginBody(string loginId, string secret, AuthType authType)
        {
            object body = null;

            if (authType == AuthType.ClientId || authType == AuthType.Application)
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

        public T GetEntity<T>(string collection, string name)
        {
            var response = _request.Execute(string.Format("/{0}/{1}", collection, name), Method.GET, accessToken: AccessToken);

            if (response.StatusCode == HttpStatusCode.NotFound)
                return default(T);
            ValidateResponse(response);

            var entity = JsonConvert.DeserializeObject<UsergridGetResponse<T>>(response.Content);

            return entity.Entities.FirstOrDefault();
        }

        private void ValidateResponse(IRestResponse response)
        {
            if (response.StatusCode != HttpStatusCode.OK)
            {
                var userGridError = JsonConvert.DeserializeObject<UsergridError>(response.Content);
                throw new UsergridException(userGridError);
            }
        }
    }
}