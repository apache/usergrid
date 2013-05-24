using System.Collections.Generic;
using Newtonsoft.Json;

namespace Usergrid.Sdk.Model
{
    public class UsergridGetResponse<T>
    {
        [JsonProperty(PropertyName = "cursor")] 
        public string Cursor;
        [JsonProperty(PropertyName = "entities")] 
        public IList<T> Entities;
    }
}