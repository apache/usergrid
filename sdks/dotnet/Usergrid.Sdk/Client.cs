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
        private readonly AuthType _authType;
        private readonly IUsergridRequest _request;

        public Client(string organization, string application, AuthType authType)
            : this(organization, application, authType, UserGridEndPoint, new UsergridRequest(UserGridEndPoint, organization, application))
        {
        }

        public Client(string organization, string application, AuthType authType, string uri = UserGridEndPoint)
            : this(organization, application, authType, uri, new UsergridRequest(uri, organization, application))
        {
        }

        internal Client(string organization, string application, AuthType authType, string uri = UserGridEndPoint, IUsergridRequest request = null)
        {
            _authType = authType;
            _request = request ?? new UsergridRequest(uri, organization, application);
        }

        public string AccessToken { get; private set; }

        public void Login(string loginId, string secret)
        {
            if (_authType == AuthType.None)
            {
                AccessToken = null;
                return;
            }

            var body = GetLoginBody(loginId, secret);

            var response = _request.Execute<LoginResponse>("/token", Method.POST, body);
            ValidateResponse(response);

            AccessToken = response.Data.AccessToken;
        }

        public T GetEntity<T>(string collection, string name)
        {
            var response = _request.Execute(string.Format("/{0}/{1}", collection, name), Method.GET, accessToken: AccessToken);
            ValidateResponse(response);

            var entity = JsonConvert.DeserializeObject<UsergridGetResponse<T>>(response.Content);

            return entity.Entities.FirstOrDefault();
        }


        private object GetLoginBody(string loginId, string secret)
        {
            object body = null;

            if (_authType == AuthType.ClientId || _authType == AuthType.Application)
            {
                body = new ClientIdLoginPayload
                    {
                        ClientId = loginId,
                        ClientSecret = secret
                    };
            }
            else if (_authType == AuthType.User)
            {
                body = new UserLoginPayload
                    {
                        UserName = loginId,
                        Password = secret
                    };
            }
            return body;
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