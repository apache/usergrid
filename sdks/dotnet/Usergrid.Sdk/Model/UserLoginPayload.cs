using Newtonsoft.Json;

namespace Usergrid.Sdk.Model
{
    public class UserLoginPayload
    {
        [JsonProperty(PropertyName = "grant_type")]
        public string GrantType
        {
            get { return "password"; }
        }

        [JsonProperty(PropertyName = "username")]
        public string UserName { get; set; }

        [JsonProperty(PropertyName = "password")]
        public string Password { get; set; }
    }
}