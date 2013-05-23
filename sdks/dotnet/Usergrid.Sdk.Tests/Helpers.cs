using Newtonsoft.Json;

namespace Usergrid.Sdk.Tests
{
    public static class ObjectExtensions
    {
        public static string Serialize(this object obj)
        {
            return JsonConvert.SerializeObject(obj);
        }
    }
}