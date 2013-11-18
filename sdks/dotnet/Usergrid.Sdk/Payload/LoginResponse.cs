using Newtonsoft.Json;

namespace Usergrid.Sdk.Payload
{
    internal class LoginResponse
    {
        [JsonProperty(PropertyName = "access_token")]
        public string AccessToken { get; set; }
    }
}