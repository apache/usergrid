using Newtonsoft.Json;

namespace Usergrid.Sdk.Payload
{
    internal class CancelNotificationPayload
    {
        [JsonProperty(PropertyName = "canceled")]
        internal bool Canceled { get; set; }
    }
}