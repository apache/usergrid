using Newtonsoft.Json;

namespace Usergrid.Sdk.Model
{
    public class UsergridUser : UsergridEntity
    {
        [JsonProperty("username")]
        public string UserName { get; set; }
        [JsonProperty("email")]
        public string Email { get; set; }
    }
}