using Newtonsoft.Json;

namespace Usergrid.Sdk.Model
{
    public class UsergridError
    {
        [JsonProperty(PropertyName = "error")]
        public string Error { get; set; }

        [JsonProperty(PropertyName = "exception")]
        public string Exception { get; set; }

        [JsonProperty(PropertyName = "error_description")]
        public string Description { get; set; }
    }
}