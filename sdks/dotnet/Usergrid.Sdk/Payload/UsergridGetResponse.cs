using System.Collections.Generic;
using Newtonsoft.Json;

namespace Usergrid.Sdk.Payload
{
    public class UsergridGetResponse<T>
    {
        [JsonProperty(PropertyName = "cursor")] 
		internal string Cursor;
        [JsonProperty(PropertyName = "entities")] 
		internal IList<T> Entities;
    }
}