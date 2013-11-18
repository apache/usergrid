using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using RestSharp.Serializers;

namespace Usergrid.Sdk
{
	public class RestSharpJsonSerializer : ISerializer
    {
		public string Serialize(object obj)
        {
            var serializeObject = JsonConvert.SerializeObject(obj, new JsonSerializerSettings()
                                                                       {
                                                                           ContractResolver = new CamelCasePropertyNamesContractResolver()
                                                                       });
            return serializeObject;
        }

		public string RootElement { get; set; }
		public string Namespace { get; set; }
		public string DateFormat { get; set; }
		public string ContentType { get; set; }
    }
}