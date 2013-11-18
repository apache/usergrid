using Newtonsoft.Json;

namespace Usergrid.Sdk.Payload
{
	internal class ClientIdLoginPayload
    {
        [JsonProperty(PropertyName = "grant_type")]
		internal string GrantType
        {
            get { return "client_credentials"; }
        }

        [JsonProperty(PropertyName = "client_id")]
		internal string ClientId { get; set; }

        [JsonProperty(PropertyName = "client_secret")]
		internal string ClientSecret { get; set; }
    }
}