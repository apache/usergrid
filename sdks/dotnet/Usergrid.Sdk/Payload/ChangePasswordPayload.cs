using Newtonsoft.Json;

namespace Usergrid.Sdk.Payload
{
	internal class ChangePasswordPayload
    {
        [JsonProperty(PropertyName = "oldpassword")]
		internal string OldPassword { get; set; }

        [JsonProperty(PropertyName = "newpassword")]
		internal string NewPassword { get; set; }
    }
}