using Newtonsoft.Json;

namespace Usergrid.Sdk.Payload
{
	internal class UserLoginPayload
    {
        [JsonProperty(PropertyName = "grant_type")]
		internal string GrantType
        {
            get { return "password"; }
        }

        [JsonProperty(PropertyName = "username")]
		internal string UserName { get; set; }

        [JsonProperty(PropertyName = "password")]
		internal string Password { get; set; }
    }
}