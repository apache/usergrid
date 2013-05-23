using Newtonsoft.Json;

namespace Usergrid.Sdk.Model
{
    public class LoginResponse
    {
        [JsonProperty(PropertyName = "access_token")]
        public string AccessToken { get; set; }

        //todo: expires_in
    }
}