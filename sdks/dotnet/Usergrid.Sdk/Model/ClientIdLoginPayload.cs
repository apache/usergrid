using Newtonsoft.Json;

namespace Usergrid.Sdk.Model
{
    public class ClientIdLoginPayload
    {
        [JsonProperty(PropertyName = "grant_type")]
        public string GrantType
        {
            get { return "client_credentials"; }
        }

        [JsonProperty(PropertyName = "client_id")]
        public string ClientId { get; set; }

        [JsonProperty(PropertyName = "client_secret")]
        public string ClientSecret { get; set; }
    }
}