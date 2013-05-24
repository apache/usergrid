using Newtonsoft.Json;
using RestSharp.Serializers;

namespace Usergrid.Sdk
{
    public class RestSharpJsonDeserializer : ISerializer
    {
        public string Serialize(object obj)
        {
            return JsonConvert.SerializeObject(obj);
        }

        public string RootElement { get; set; }
        public string Namespace { get; set; }
        public string DateFormat { get; set; }
        public string ContentType { get; set; }
    }
}